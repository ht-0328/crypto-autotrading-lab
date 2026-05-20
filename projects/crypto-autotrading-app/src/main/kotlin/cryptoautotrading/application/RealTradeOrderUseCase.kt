package cryptoautotrading.application

import cryptoautotrading.application.port.RealTradingExchangePort
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.realtrading.RealOrderSide
import cryptoautotrading.domain.model.realtrading.RealOrderState
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.domain.service.realtrading.RealTradingSafetyChecker
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * リアル取引の注文処理を担当するUseCase
 *
 * @property exchangePort リアル取引の取引所操作を行うポート
 * @property safetyChecker 実注文前安全チェックを行うサービス
 */
class RealTradeOrderUseCase(
    private val exchangePort: RealTradingExchangePort? = null,
    private val safetyChecker: RealTradingSafetyChecker = RealTradingSafetyChecker()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 判定結果と設定に基づいてリアル注文を実行するかどうかを判断し、処理を行う。
     *
     * BUY_CANDIDATE の場合、安全チェックを通過した場合のみ GMO Private API の placeOrder を呼び、
     * 返ってきた orderId を state の realTrading.latestOrder に保存する。
     *
     * @param decision 戦略判定によって下された売買判定結果
     * @param config リアル取引に関する設定
     * @param tradeAmount 1回あたりの注文予定金額(JPY)
     * @param symbol 銘柄名
     * @param currentState 現在のシミュレーション(およびリアル取引)の状態
     * @param currentPrice 現在の市場価格
     * @return 処理後のシミュレーション状態
     */
    suspend fun executeOrderIfNeeded(
        decision: TradeDecision,
        config: RealTradingConfig,
        tradeAmount: Int,
        symbol: String,
        currentState: SimulationState,
        currentPrice: BigDecimal
    ): SimulationState {
        if (config.dryRun || !config.realTradeEnabled) {
            logger.debug { "Real trading is disabled or in dry-run mode. Skipping real trade execution." }
            return currentState
        }

        if (decision.action == TradeAction.BUY_CANDIDATE) {
            logger.info { "リアル取引: BUY_CANDIDATE を検知しました。安全チェックを開始します。" }

            if (exchangePort == null) {
                logger.warn { "リアル取引: exchangePort が設定されていないため、実注文処理をスキップします。" }
                return currentState
            }

            try {
                // GMO Private API から現在のアセットとアクティブオーダーを取得
                val currentHoldingAssets = exchangePort.getAssets()

                // JPY の available を取得
                val jpyAsset = currentHoldingAssets.find { it.symbol == "JPY" }
                val jpyAvailable = jpyAsset?.available ?: BigDecimal.ZERO

                if (jpyAvailable < BigDecimal(tradeAmount)) {
                    val reason = "JPY available (${jpyAvailable}) が注文予定額 ($tradeAmount) 未満です"
                    logger.warn { "安全チェックNG: $reason" }
                    return currentState
                }

                // 現在の銘柄の保有チェック
                val isHoldingCrypto = currentHoldingAssets.any { it.symbol == symbol && it.amount > BigDecimal.ZERO }
                val checkAssets = if (isHoldingCrypto) {
                    currentHoldingAssets.filter { it.symbol == symbol }
                } else {
                    emptyList()
                }

                val activeOrders = exchangePort.getActiveOrders(symbol)

                val safetyCheckResult = safetyChecker.checkPreOrderSafety(
                    config = config,
                    tradeAmount = tradeAmount,
                    state = currentState,
                    currentHoldingAssets = checkAssets,
                    activeOrders = activeOrders,
                    currentPrice = currentPrice
                )

                if (!safetyCheckResult.passed) {
                    logger.info { "リアル取引: 安全チェックで注文が見送られました。理由: ${safetyCheckResult.reason}" }
                    return currentState
                }

                logger.info { "リアル取引: 安全チェックを通過しました。注文処理を開始します。" }

                // 注文数量の計算 (注文予定額 / 現在価格、切り捨て)
                val size = BigDecimal(tradeAmount).divide(currentPrice, 8, RoundingMode.DOWN)

                // 注文実行
                val acceptedOrder = exchangePort.placeOrder(
                    symbol = symbol,
                    side = "BUY",
                    executionType = "MARKET",
                    size = size,
                    price = null
                )

                val nowStr = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                // 1日の累計注文額を加算し、日付を更新する
                // 現状、同日内の注文受付であれば無条件に加算する（キャンセルされても加算されたままになるが安全寄りの設計とする）
                val todayStr = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isSameDay = currentState.realTrading.dailyOrderedDate == todayStr
                val newDailyOrderedJpy = if (isSameDay) {
                    currentState.realTrading.dailyOrderedJpy.add(BigDecimal(tradeAmount))
                } else {
                    BigDecimal(tradeAmount)
                }

                // orderId を保存した新しい状態を生成
                val newLatestOrder = RealOrderState(
                    orderId = acceptedOrder.orderId,
                    symbol = symbol,
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED, // まずは ORDERED としておく。約定確認で EXECUTED になる。
                    requestedAmountJpy = BigDecimal(tradeAmount),
                    requestedSize = size,
                    requestedPrice = currentPrice,
                    orderedAt = nowStr
                )

                val newRealTradingState = currentState.realTrading.copy(
                    latestOrder = newLatestOrder,
                    dailyOrderedDate = todayStr,
                    dailyOrderedJpy = newDailyOrderedJpy
                )

                logger.info { "リアル取引: 注文受付完了。orderId: ${acceptedOrder.orderId} を保存しました。" }

                // isHolding はここでは true にしない。約定確認時に行う。
                return currentState.copy(
                    realTrading = newRealTradingState
                )

            } catch (e: Exception) {
                logger.error(e) { "リアル取引: 注文処理中にエラーが発生しました。" }
                // エラー発生時は状態を停止モードにする
                val newRealTradingState = currentState.realTrading.copy(
                    isStopped = true,
                    stopReason = e.message ?: "Unknown error",
                    stoppedAt = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
                return currentState.copy(
                    realTrading = newRealTradingState
                )
            }
        } else if (decision.action == TradeAction.SELL_CANDIDATE) {
            logger.info { "リアル取引: SELL_CANDIDATE を検知しましたが、現行フェーズでは売り注文は実行しません。" }
            return currentState
        } else {
            logger.debug { "Trade action is not BUY_CANDIDATE (${decision.action}). No real trade action taken." }
            return currentState
        }
    }
}
