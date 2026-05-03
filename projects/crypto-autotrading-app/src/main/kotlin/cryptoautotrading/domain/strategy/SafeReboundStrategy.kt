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
 * 購入価格を基準に利確・損切りを行う安全寄りの反発狙い戦略
 *
 * @property config 取引設定
 */
class SafeReboundStrategy(
    private val config: TradingConfig
) : TradingStrategy {

    private val logger = KotlinLogging.logger {}

    override fun judge(klines: List<Kline>, currentState: SimulationState): TradeDecision {
        val isHolding = currentState.isHolding
        val buyPrice = currentState.buyPrice

        logger.info { "売買判定を開始します (SafeReboundStrategy)" }
        logger.debug { "入力値: K線データ件数=${klines.size}, 保有状態=$isHolding, 購入価格=$buyPrice" }

        // 直近12本のデータのみを使用（1時間が対象）
        val recentKlines = klines.sortedBy { it.openTime }.takeLast(12)

        if (recentKlines.size < 12) {
            return createDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "データ不足（12本未満）")
        }

        return if (!isHolding) {
            judgeEntry(recentKlines)
        } else {
            judgeExit(recentKlines.last().close.toBigDecimal(), buyPrice)
        }
    }

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

        return createDecision(TradeAction.BUY_CANDIDATE, "1時間下落後の反発確認")
    }

    private fun judgeExit(latestClose: BigDecimal, buyPrice: BigDecimal): TradeDecision {
        if (buyPrice <= BigDecimal.ZERO) {
            return createDecision(TradeAction.HOLDING, "購入価格が未設定")
        }

        val sellThresholdBD = config.sellThreshold.toBigDecimal()

        // 利確: latestClose >= buyPrice * (1 + config.sellThreshold)
        val takeProfitPrice = buyPrice * (BigDecimal.ONE + sellThresholdBD)
        if (latestClose >= takeProfitPrice) {
            return createDecision(TradeAction.SELL_CANDIDATE, "利確")
        }

        // 損切り: latestClose <= buyPrice * (1 - config.sellThreshold)
        val stopLossPrice = buyPrice * (BigDecimal.ONE - sellThresholdBD)
        if (latestClose <= stopLossPrice) {
            return createDecision(TradeAction.SELL_CANDIDATE, "損切り")
        }

        return createDecision(TradeAction.HOLDING, "条件に合致せず（保有継続）")
    }

    private fun isSharpChange(recentKlines: List<Kline>): Boolean {
        // 急変動フィルター (直近15分 = 3本)
        val recent3Klines = recentKlines.takeLast(3)
        val maxHigh3 = recent3Klines.maxOfOrNull { it.high.toBigDecimal() } ?: BigDecimal.ZERO
        val minLow3 = recent3Klines.minOfOrNull { it.low.toBigDecimal() } ?: BigDecimal.ONE

        if (minLow3 <= BigDecimal.ZERO) return false

        val sharpChangeRate = (maxHigh3 - minLow3).divide(minLow3, 8, RoundingMode.HALF_UP)
        val sharpChangeThresholdBD = config.sharpChangeThreshold.toBigDecimal()

        return sharpChangeRate >= sharpChangeThresholdBD
    }

    private fun calculateHourChange(latestClose: BigDecimal, oldestOpen: BigDecimal): BigDecimal {
        return if (oldestOpen > BigDecimal.ZERO) {
            (latestClose - oldestOpen).divide(oldestOpen, 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    private fun isReboundKline(latestKline: Kline): Boolean {
        val latestOpen = latestKline.open.toBigDecimal()
        val latestClose = latestKline.close.toBigDecimal()
        val latestLow = latestKline.low.toBigDecimal()

        // 1. 陽線である
        val isYang = latestClose > latestOpen

        // 2. 下ヒゲが実体より長い
        val body = (latestClose - latestOpen).abs()
        val lowerWick = latestOpen.min(latestClose) - latestLow
        val hasLongLowerWick = lowerWick > body

        return isYang || hasLongLowerWick
    }

    private fun createDecision(action: TradeAction, reason: String): TradeDecision {
        val decision = TradeDecision(action, reason)
        logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
