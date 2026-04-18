package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import kotlin.math.max
import kotlin.math.min

/**
 * 売買判定を行う戦略クラス
 */
class TradingStrategy {

    /**
     * K線データと保有状態から売買判定を行う
     *
     * @param klines K線データのリスト（直近のデータが含まれること）
     * @param isHolding 現在ポジションを保有しているかどうか
     * @return 判定結果
     */
    fun judge(klines: List<Kline>, isHolding: Boolean): TradeDecision {
        // 直近12本のデータのみを使用（1時間が対象）
        val recentKlines = klines.sortedBy { it.openTime }.takeLast(12)

        if (recentKlines.size < 12) {
            return TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "データ不足（12本未満）")
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

        if (hourFluctuation < 0.003) {
            return TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "直近1時間の変動が 0.3%未満")
        }

        // 2. 直近15分の変動チェック
        // 最新を含め直近3本が15分に相当する (最新、1つ前、2つ前)
        val recent15MinOpens = opens.takeLast(3)
        val startOf15MinOpen = recent15MinOpens.first()
        val change15Min = (latestClose - startOf15MinOpen) / startOf15MinOpen

        if (change15Min <= -0.01) {
            return TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "直近15分で 1.0%以上下落")
        }

        if (change15Min >= 0.01) {
            return TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "直近15分で 1.0%以上上昇")
        }

        // 3. 1時間の変動による売買サインの判定
        val hourChange = (latestClose - oldestOpen) / oldestOpen

        if (!isHolding && hourChange <= -0.005) {
            return TradeDecision(TradeAction.BUY_CANDIDATE, "0.5%下落")
        }

        if (isHolding && hourChange >= 0.005) {
            return TradeDecision(TradeAction.SELL_CANDIDATE, "0.5%上昇")
        }

        return TradeDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "条件に合致せず")
    }
}
