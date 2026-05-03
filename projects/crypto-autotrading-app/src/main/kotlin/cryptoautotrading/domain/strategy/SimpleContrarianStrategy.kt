package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import io.github.oshai.kotlinlogging.KotlinLogging

import cryptoautotrading.domain.model.TradingConfig

import cryptoautotrading.domain.model.SimulationState

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

        // 直近12本のデータのみを使用（1時間が対象）
        val recentKlines = klines.sortedBy { it.openTime }.takeLast(12)

        if (recentKlines.size < 12) {
            val decision = TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "データ不足（12本未満）")
            logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
            return decision
        }

        // K線の各価格はStringなのでDoubleに変換する
        val closes = recentKlines.map { it.close.toDouble() }
        val opens = recentKlines.map { it.open.toDouble() }
        val highs = recentKlines.map { it.high.toDouble() }
        val lows = recentKlines.map { it.low.toDouble() }

        val latestClose = closes.last()
        val oldestOpen = opens.first()

        // 1. 直近1時間の変動幅のチェック (max(high) - min(low)) / min(low)
        val maxHigh = highs.maxOrNull() ?: 0.0
        val minLow = lows.minOrNull() ?: 1.0
        val hourFluctuation = (maxHigh - minLow) / minLow

        if (hourFluctuation < config.volatilityThreshold) {
            val decision = TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "直近1時間の変動が ${config.volatilityThreshold * 100}%未満")
            logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
            return decision
        }

        // 2. 直近15分の変動チェック
        // 最新を含め直近3本が15分に相当する (最新、1つ前、2つ前)
        val recent15MinOpens = opens.takeLast(3)
        val startOf15MinOpen = recent15MinOpens.first()
        val change15Min = (latestClose - startOf15MinOpen) / startOf15MinOpen

        if (change15Min <= -config.sharpChangeThreshold) {
            val decision = TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "直近15分で ${config.sharpChangeThreshold * 100}%以上下落")
            logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
            return decision
        }

        if (change15Min >= config.sharpChangeThreshold) {
            val decision = TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "直近15分で ${config.sharpChangeThreshold * 100}%以上上昇")
            logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
            return decision
        }

        // 3. 1時間の変動による売買サインの判定
        val hourChange = (latestClose - oldestOpen) / oldestOpen

        if (!isHolding && hourChange <= -config.buyThreshold) {
            val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "${config.buyThreshold * 100}%下落")
            logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
            return decision
        }

        if (isHolding && hourChange >= config.sellThreshold) {
            val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "${config.sellThreshold * 100}%上昇")
            logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
            return decision
        }

        val decision = TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "条件に合致せず")
        logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
