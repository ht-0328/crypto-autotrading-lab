package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.TradingConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * CooldownReboundStrategy に加え、短期トレンド（MA5）の上抜け確認を買い条件とする戦略
 *
 * @property config 取引設定
 */
class TrendConfirmReboundStrategy(
    private val config: TradingConfig
) : TradingStrategy {

    private val logger = KotlinLogging.logger {}

    /**
     * 売買判定を行う。
     *
     * @param klines K線データのリスト
     * @param currentState 現在のシミュレーション状態
     * @return 判定結果
     */
    override fun judge(klines: List<Kline>, currentState: SimulationState): TradeDecision {
        val isHolding = currentState.isHolding
        val buyPrice = currentState.buyPrice

        logger.debug { "売買判定を開始します (TrendConfirmReboundStrategy)" }
        logger.debug { "入力値: K線データ件数=${klines.size}, 保有状態=$isHolding, 購入価格=$buyPrice, 最終損切り時刻=${currentState.lastStopLossTime}" }

        // MA5計算には最低6本（最新と1本前それぞれの5本分）が必要。
        // また、急変動判定などで十分な本数（CooldownReboundStrategy同様12本）を利用する
        val recentKlines = klines.sortedBy { it.openTime }.takeLast(12)

        if (recentKlines.size < 12) {
            return createDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "データ不足（12本未満）")
        }

        if (isHolding) {
            return judgeExit(recentKlines.last().close.toBigDecimal(), buyPrice)
        }

        if (isCooldownPeriod(klines, currentState.lastStopLossTime)) {
            return createDecision(TradeAction.SKIP, "クールダウン期間中")
        }

        return judgeEntry(recentKlines)
    }

    /**
     * 現在が損切り直後のクールダウン期間中かどうかを判定する。
     */
    private fun isCooldownPeriod(klines: List<Kline>, lastStopLossTime: String): Boolean {
        if (lastStopLossTime.isBlank()) {
            return false
        }

        val sortedKlines = klines.sortedBy { it.openTime }
        val stopLossIndex = sortedKlines.indexOfLast { it.openTime == lastStopLossTime }

        if (stopLossIndex == -1) {
            return false
        }

        val currentIndex = sortedKlines.lastIndex
        val elapsedKlineCount = currentIndex - stopLossIndex

        if (elapsedKlineCount < 0) return false

        return elapsedKlineCount <= config.cooldownLength
    }

    /**
     * 新規購入（エントリー）の判定を行う。
     */
    private fun judgeEntry(recentKlines: List<Kline>): TradeDecision {
        val latestKline = recentKlines.last()
        val latestClose = latestKline.close.toBigDecimal()

        if (isSharpChange(recentKlines)) {
            return createDecision(TradeAction.SKIP, "急変動（直近15分）")
        }

        val oldestOpen = recentKlines.first().open.toBigDecimal()
        val hourChange = calculateHourChange(latestClose, oldestOpen)
        val buyThresholdBD = config.buyThreshold.toBigDecimal()

        if (hourChange > -buyThresholdBD) {
            return createDecision(TradeAction.SKIP, "条件に合致せず（1時間下落不足）")
        }

        if (!isReboundKline(latestKline)) {
            return createDecision(TradeAction.SKIP, "反発未確認")
        }

        if (!isTrendConfirm(recentKlines)) {
            return createDecision(TradeAction.SKIP, "MA5上抜け未確認")
        }

        return createDecision(TradeAction.BUY_CANDIDATE, "1時間下落後の反発確認およびMA5上抜け確認")
    }

    /**
     * 売却（エグジット）の判定を行う。
     */
    private fun judgeExit(latestClose: BigDecimal, buyPrice: BigDecimal): TradeDecision {
        if (buyPrice <= BigDecimal.ZERO) {
            return createDecision(TradeAction.HOLDING, "購入価格が未設定")
        }

        val sellThresholdBD = config.sellThreshold.toBigDecimal()

        // 利確
        val takeProfitPrice = buyPrice * (BigDecimal.ONE + sellThresholdBD)
        if (latestClose >= takeProfitPrice) {
            return createDecision(TradeAction.SELL_CANDIDATE, "利確")
        }

        // 損切り
        val stopLossPrice = buyPrice * (BigDecimal.ONE - sellThresholdBD)
        if (latestClose <= stopLossPrice) {
            return createDecision(TradeAction.SELL_CANDIDATE, "損切り")
        }

        return createDecision(TradeAction.HOLDING, "条件に合致せず（保有継続）")
    }

    /**
     * 直近15分（K線3本分）で価格が急変動しているかを確認する。
     */
    private fun isSharpChange(recentKlines: List<Kline>): Boolean {
        val recent3Klines = recentKlines.takeLast(3)
        val maxHigh3 = recent3Klines.maxOfOrNull { it.high.toBigDecimal() } ?: BigDecimal.ZERO
        val minLow3 = recent3Klines.minOfOrNull { it.low.toBigDecimal() } ?: BigDecimal.ONE

        if (minLow3 <= BigDecimal.ZERO) return false

        val sharpChangeRate = (maxHigh3 - minLow3).divide(minLow3, 8, RoundingMode.HALF_UP)
        val sharpChangeThresholdBD = config.sharpChangeThreshold.toBigDecimal()

        return sharpChangeRate >= sharpChangeThresholdBD
    }

    /**
     * 直近1時間での価格の変動率を計算する。
     */
    private fun calculateHourChange(latestClose: BigDecimal, oldestOpen: BigDecimal): BigDecimal {
        return if (oldestOpen > BigDecimal.ZERO) {
            (latestClose - oldestOpen).divide(oldestOpen, 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * 最新のK線が反発のサインを示しているかを確認する。
     */
    private fun isReboundKline(latestKline: Kline): Boolean {
        val latestOpen = latestKline.open.toBigDecimal()
        val latestClose = latestKline.close.toBigDecimal()
        val latestLow = latestKline.low.toBigDecimal()

        val isYang = latestClose > latestOpen

        val body = (latestClose - latestOpen).abs()
        val lowerWick = latestOpen.min(latestClose) - latestLow
        val hasLongLowerWick = lowerWick > body

        return isYang || hasLongLowerWick
    }

    /**
     * MA5（直近5本の終値平均）の上抜け確認を行う。
     * - 最新の終値が最新の MA5 より上
     * - 直前の終値は直前の MA5 以下
     * - 最新の MA5 が直前の MA5 より上
     */
    private fun isTrendConfirm(recentKlines: List<Kline>): Boolean {
        if (recentKlines.size < 6) return false

        // 最新のMA5を計算 (直近5本)
        val latest5Klines = recentKlines.takeLast(5)
        val latestMa5 = latest5Klines.map { it.close.toBigDecimal() }
            .reduce { acc, bigDecimal -> acc + bigDecimal }
            .divide(BigDecimal(5), 8, RoundingMode.HALF_UP)

        // 直前のMA5を計算 (最新の1本を除いた直近5本)
        val previous5Klines = recentKlines.dropLast(1).takeLast(5)
        val previousMa5 = previous5Klines.map { it.close.toBigDecimal() }
            .reduce { acc, bigDecimal -> acc + bigDecimal }
            .divide(BigDecimal(5), 8, RoundingMode.HALF_UP)

        val latestClose = recentKlines.last().close.toBigDecimal()
        val previousClose = recentKlines[recentKlines.lastIndex - 1].close.toBigDecimal()

        // 最新の終値 > 最新の MA5
        val condition1 = latestClose > latestMa5
        // 直前の終値 <= 直前の MA5
        val condition2 = previousClose <= previousMa5
        // 最新の MA5 > 直前の MA5
        val condition3 = latestMa5 > previousMa5

        return condition1 && condition2 && condition3
    }

    /**
     * 売買アクションと理由文字列をもとに、判定結果を生成する。
     */
    private fun createDecision(action: TradeAction, reason: String): TradeDecision {
        val decision = TradeDecision(action, reason)
        logger.debug { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
