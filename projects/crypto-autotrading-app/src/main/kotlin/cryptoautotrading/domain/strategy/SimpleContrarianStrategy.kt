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

    /**
     * K線データのリストから直近1時間の分析に必要な最新12本を抽出する。
     *
     * @param klines 元となるK線データのリスト
     * @return 時間順にソートされた最新12件のK線リスト
     */
    private fun selectRecentKlines(klines: List<Kline>): List<Kline> {
        return klines.sortedBy { it.openTime }.takeLast(12)
    }

    /**
     * K線リストの特定の項目（高値、安値など）を抽出し、Double型のリストに変換する。
     *
     * @param klines K線データのリスト
     * @param selector 抽出する項目を指定する関数
     * @return 抽出・変換されたDoubleのリスト
     */
    private fun toDoubleList(klines: List<Kline>, selector: (Kline) -> String): List<Double> {
        return klines.map { selector(it).toDouble() }
    }

    /**
     * 与えられた高値と安値のリストから、直近1時間での最大変動幅（ボラティリティ）を計算する。
     *
     * @param highs 直近1時間の高値のリスト
     * @param lows 直近1時間の安値のリスト
     * @return 変動幅（最大高値 - 最小安値）/ 最小安値 の割合
     */
    private fun calculateHourFluctuation(highs: List<Double>, lows: List<Double>): Double {
        val maxHigh = highs.maxOrNull() ?: 0.0
        val minLow = lows.minOrNull() ?: 1.0
        return (maxHigh - minLow) / minLow
    }

    /**
     * 直近15分（K線3本分）での価格変動率を計算する。
     * 15分前の始値と現在の終値の変化割合を算出する。
     *
     * @param opens 始値のリスト
     * @param latestClose 最新の終値
     * @return 直近15分の価格変動率
     */
    private fun calculateRecent15MinuteChange(opens: List<Double>, latestClose: Double): Double {
        val recent15MinOpens = opens.takeLast(3)
        val startOf15MinOpen = recent15MinOpens.first()
        return (latestClose - startOf15MinOpen) / startOf15MinOpen
    }

    /**
     * 直近15分の急変動（急騰・急落）をもとに売買判定を行う。
     *
     * @param change15Min 直近15分の価格変動率
     * @param isHolding 現在ポジションを保有しているか
     * @return 急変動による判定が行われた場合はTradeDecision、そうでない場合はnull
     */
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

    /**
     * 直近1時間の価格変動率を計算する。
     * 1時間前の始値と現在の終値を比較する。
     *
     * @param latestClose 最新の終値
     * @param oldestOpen 1時間前の始値
     * @return 1時間の価格変動率
     */
    private fun calculateHourChange(latestClose: Double, oldestOpen: Double): Double {
        return (latestClose - oldestOpen) / oldestOpen
    }

    /**
     * 1時間の変動率をもとに、通常の売買判定（逆張り）を行う。
     * 大きな下落があれば買い、大きな上昇があれば売り（利確）と判断する。
     *
     * @param hourChange 1時間の価格変動率
     * @param isHolding 現在ポジションを保有しているか
     * @return 条件を満たした場合はTradeDecision、満たさない場合はnull
     */
    private fun judgeByHourChange(hourChange: Double, isHolding: Boolean): TradeDecision? {
        if (!isHolding && hourChange <= -config.buyThreshold) {
            return createDecision(TradeAction.BUY_CANDIDATE, "${config.buyThreshold * 100}%下落")
        }

        if (isHolding && hourChange >= config.sellThreshold) {
            return createDecision(TradeAction.SELL_CANDIDATE, "${config.sellThreshold * 100}%上昇")
        }

        return null
    }

    /**
     * 売買アクションとその理由をもとに、判定結果のオブジェクトを生成する。
     * 判定結果はログにも出力する。
     *
     * @param action 決定した売買アクション
     * @param reason アクションを決定した理由
     * @return 生成された判定結果
     */
    private fun createDecision(action: TradeAction, reason: String): TradeDecision {
        val decision = TradeDecision(action, reason)
        logger.info { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
