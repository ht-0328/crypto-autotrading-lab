package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.TradingConfig
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * シンプルな逆張りで売買判定を行う戦略クラス（既存ロジック）
 *
 * @property config 取引設定
 */
class SimpleContrarianStrategy(
    private val config: TradingConfig
) : TradingStrategy {

    private val logger = KotlinLogging.logger {}

    /**
     * K線データと保有状態から売買判定を行う
     *
     * @param klines K線データのリスト（直近のデータが含まれること）
     * @param currentState 現在のシミュレーション状態
     * @return 判定結果
     */
    override fun judge(
        klines: List<Kline>,
        currentState: SimulationState
    ): TradeDecision {
        val isHolding = currentState.isHolding
        logger.info { "売買判定を開始します (SimpleContrarianStrategy)" }
        logger.debug { "入力値: K線データ件数=${klines.size}, 保有状態=$isHolding" }

        val recentKlines = selectRecentKlines(klines)

        if (recentKlines.size < 12) {
            val action = if (isHolding) TradeAction.HOLDING else TradeAction.SKIP
            return createDecision(action, "データ不足（12本未満）")
        }

        val closes = toDoubleList(recentKlines) { it.close }
        val opens = toDoubleList(recentKlines) { it.open }
        val highs = toDoubleList(recentKlines) { it.high }
        val lows = toDoubleList(recentKlines) { it.low }

        val latestClose = closes.last()
        val oldestOpen = opens.first()

        // 1. 直近1時間の変動幅のチェック
        val hourFluctuation = calculateHourFluctuation(highs, lows)
        if (hourFluctuation < config.volatilityThreshold) {
            val action = if (isHolding) TradeAction.HOLDING else TradeAction.SKIP
            return createDecision(action, "直近1時間の変動が ${config.volatilityThreshold * 100}%未満")
        }

        // 2. 直近15分の変動チェック
        val change15Min = calculateRecent15MinuteChange(opens, latestClose)
        val sharpChangeDecision = judgeBySharpChange(change15Min, isHolding)
        if (sharpChangeDecision != null) {
            return sharpChangeDecision
        }

        // 3. 1時間の変動による売買サインの判定
        val hourChange = calculateHourChange(latestClose, oldestOpen)
        val hourChangeDecision = judgeByHourChange(hourChange, isHolding)
        if (hourChangeDecision != null) {
            return hourChangeDecision
        }

        val action = if (isHolding) TradeAction.HOLDING else TradeAction.SKIP
        return createDecision(action, "条件に合致せず")
    }

    private fun selectRecentKlines(klines: List<Kline>): List<Kline> {
        return klines.sortedBy { it.openTime }.takeLast(12)
    }

    private fun toDoubleList(klines: List<Kline>, selector: (Kline) -> String): List<Double> {
        return klines.map { selector(it).toDouble() }
    }

    private fun calculateHourFluctuation(highs: List<Double>, lows: List<Double>): Double {
        val maxHigh = highs.maxOrNull() ?: 0.0
        val minLow = lows.minOrNull() ?: 1.0
        return (maxHigh - minLow) / minLow
    }

    private fun calculateRecent15MinuteChange(opens: List<Double>, latestClose: Double): Double {
        val recent15MinOpens = opens.takeLast(3)
        val startOf15MinOpen = recent15MinOpens.first()
        return (latestClose - startOf15MinOpen) / startOf15MinOpen
    }

    private fun judgeBySharpChange(change15Min: Double, isHolding: Boolean): TradeDecision? {
        if (change15Min <= -config.sharpChangeThreshold) {
            val action = if (isHolding) TradeAction.HOLDING else TradeAction.SKIP
            return createDecision(action, "直近15分で ${config.sharpChangeThreshold * 100}%以上下落")
        }

        if (change15Min >= config.sharpChangeThreshold) {
            val action = if (isHolding) TradeAction.HOLDING else TradeAction.SKIP
            return createDecision(action, "直近15分で ${config.sharpChangeThreshold * 100}%以上上昇")
        }

        return null
    }

    private fun calculateHourChange(latestClose: Double, oldestOpen: Double): Double {
        return (latestClose - oldestOpen) / oldestOpen
    }

    private fun judgeByHourChange(hourChange: Double, isHolding: Boolean): TradeDecision? {
        if (!isHolding && hourChange <= -config.buyThreshold) {
            return createDecision(TradeAction.BUY_CANDIDATE, "${config.buyThreshold * 100}%下落")
        }

        if (isHolding && hourChange >= config.sellThreshold) {
            return createDecision(TradeAction.SELL_CANDIDATE, "${config.sellThreshold * 100}%上昇")
        }

        return null
    }

    private fun createDecision(action: TradeAction, reason: String): TradeDecision {
        val decision = TradeDecision(action, reason)
        logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
