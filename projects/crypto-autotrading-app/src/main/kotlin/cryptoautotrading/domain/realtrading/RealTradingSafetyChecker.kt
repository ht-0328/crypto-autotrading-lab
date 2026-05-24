package cryptoautotrading.domain.realtrading

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.domain.model.realtrading.SafetyCheckResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal

/**
 * リアル注文前の安全確認を行うサービスクラス
 */
class RealTradingSafetyChecker {
    private val logger = KotlinLogging.logger {}

    /**
     * 実注文前の安全チェックを行う。
     * いずれかの条件を満たさない場合は注文不可とし、falseと理由を返す。
     *
     * @param config リアル取引設定
     * @param tradeAmount 1回あたりの注文予定金額(JPY)
     * @param state 現在のシミュレーション(およびリアル取引)の状態
     * @param currentHoldingAssets GMO Private APIから取得した現在の保有資産のリスト
     * @param activeOrders GMO Private APIから取得した現在の未約定注文のリスト
     * @param currentPrice 現在の市場価格
     * @return SafetyCheckResult 注文可否と理由
     */
    fun checkPreOrderSafety(
        config: RealTradingConfig,
        tradeAmount: Int,
        state: SimulationState,
        currentHoldingAssets: List<ExchangeAsset>,
        activeOrders: List<ExchangeActiveOrder>,
        currentPrice: BigDecimal
    ): SafetyCheckResult {

        // 1. 強制停止フラグのチェック
        if (state.realTrading.isStopped) {
            val reason = "realTrading.isStopped=true"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }

        // 2. 二重注文・多重保有防止チェック
        if (state.isHolding || currentHoldingAssets.isNotEmpty()) {
            val reason = "既に保有中"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }

        // 3. 未確認・未約定注文チェック
        val latestOrderStatus = state.realTrading.latestOrder?.status
        val hasUnconfirmedOrder = latestOrderStatus == RealOrderStatus.WAITING ||
                                  latestOrderStatus == RealOrderStatus.ORDERED ||
                                  latestOrderStatus == RealOrderStatus.UNCONFIRMED

        if (hasUnconfirmedOrder || activeOrders.isNotEmpty()) {
            val reason = "未確認または受付中の注文が存在"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }

        // 4. 1回あたりの注文上限チェック
        if (config.maxOrderJpy == null) {
            val reason = "max_order_jpyが未設定"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }
        if (tradeAmount > config.maxOrderJpy) {
            val reason = "trade_amountがmax_order_jpy超過"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }

        // 5. 1日あたりの累計注文上限チェック
        if (config.maxDailyOrderJpy == null) {
            val reason = "max_daily_order_jpyが未設定"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }
        val currentDailyOrdered = state.realTrading.dailyOrderedJpy
        val newDailyTotal = currentDailyOrdered.add(BigDecimal(tradeAmount))
        if (newDailyTotal > BigDecimal(config.maxDailyOrderJpy)) {
            val reason = "1日の注文限度額超過"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }

        // 6. 最大保有金額(ポジション)のチェック
        if (config.maxPositionJpy == null) {
            val reason = "max_position_jpyが未設定"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }
        val currentHoldingValue = state.holdingAmount.multiply(currentPrice)
        val newPositionTotal = currentHoldingValue.add(BigDecimal(tradeAmount))
        if (newPositionTotal > BigDecimal(config.maxPositionJpy)) {
            val reason = "保有金額と注文予定額の合計がmax_position_jpy超過"
            logger.warn { "安全チェックNG: $reason" }
            return SafetyCheckResult(passed = false, reason = reason)
        }

        return SafetyCheckResult(passed = true)
    }
}
