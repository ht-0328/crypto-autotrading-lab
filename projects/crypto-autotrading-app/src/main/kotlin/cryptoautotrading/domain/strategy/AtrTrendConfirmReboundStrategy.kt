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
 * TrendConfirmReboundStrategy をベースに、売り条件（利確・損切りライン）を
 * ATR (Average True Range) を用いて動的に調整する戦略。
 *
 * 買い条件は TrendConfirmReboundStrategy と同じですが、
 * 購入時に ATR を計算し、その値と設定された倍率を用いて利確・損切り幅を決定します。
 *
 * @property config 取引設定
 */
class AtrTrendConfirmReboundStrategy(
    private val config: TradingConfig
) : TradingStrategy {

    private val logger = KotlinLogging.logger {}

    /**
     * K線データと現在のシミュレーション状態から売買判定を行います。
     *
     * klines を openTime 順に並べ替え、必要な直近のK線を使用します。
     * MA5計算および1時間下落判定のために最低12本、ATR計算のために設定値＋1本のデータが必要です。
     * 必要なデータが不足している場合は、保有中なら HOLDING を、非保有なら SKIP を返します。
     *
     * @param klines K線データのリスト
     * @param currentState 現在のシミュレーション状態
     * @return 判定結果
     */
    override fun judge(klines: List<Kline>, currentState: SimulationState): TradeDecision {
        val isHolding = currentState.isHolding
        val buyPrice = currentState.buyPrice

        logger.debug { "売買判定を開始します (AtrTrendConfirmReboundStrategy)" }
        logger.debug { "入力値: K線データ件数=${klines.size}, 保有状態=$isHolding, 購入価格=$buyPrice, 最終損切り時刻=${currentState.lastStopLossTime}" }

        val requiredKlines = maxOf(12, config.atrLength + 1)
        val recentKlines = klines.sortedBy { it.openTime }.takeLast(requiredKlines)

        if (recentKlines.size < requiredKlines) {
            return createDecision(if (isHolding) TradeAction.HOLDING else TradeAction.SKIP, "データ不足（${requiredKlines}本未満）")
        }

        if (isHolding) {
            return judgeExit(recentKlines.last().close.toBigDecimal(), buyPrice, currentState.entryAtr)
        }

        if (isCooldownPeriod(klines, currentState.lastStopLossTime)) {
            return createDecision(TradeAction.SKIP, "クールダウン期間中")
        }

        return judgeEntry(recentKlines)
    }

    /**
     * 現在が損切り直後のクールダウン期間中かどうかを判定します。
     *
     * @param klines K線データ全体
     * @param lastStopLossTime 最後に損切りした時刻
     * @return クールダウン期間中の場合は true、それ以外は false
     */
    private fun isCooldownPeriod(klines: List<Kline>, lastStopLossTime: String): Boolean {
        if (lastStopLossTime.isBlank()) return false
        val sortedKlines = klines.sortedBy { it.openTime }
        val stopLossIndex = sortedKlines.indexOfLast { it.openTime == lastStopLossTime }
        if (stopLossIndex == -1) return false
        val elapsedKlineCount = sortedKlines.lastIndex - stopLossIndex
        if (elapsedKlineCount < 0) return false
        return elapsedKlineCount <= config.cooldownLength
    }

    /**
     * 新規購入（エントリー）の判定を行います。
     *
     * 直近のK線データを使用して、急変動の有無、1時間の価格下落、反発サイン、
     * およびMA5（直近5本の終値平均）の上抜けを確認します。
     * すべての条件を満たした場合、購入時のATRを計算し、買い候補として判定結果を返します。
     *
     * @param recentKlines 直近のK線データ
     * @return 判定結果 (BUY_CANDIDATE または SKIP)
     */
    private fun judgeEntry(recentKlines: List<Kline>): TradeDecision {
        val latestKline = recentKlines.last()
        val latestClose = latestKline.close.toBigDecimal()

        if (isSharpChange(recentKlines)) {
            return createDecision(TradeAction.SKIP, "急変動（直近15分）")
        }

        val last12Klines = recentKlines.takeLast(12)
        val oldestOpen = last12Klines.first().open.toBigDecimal()
        val hourChange = calculateHourChange(latestClose, oldestOpen)
        val buyThresholdBD = config.buyThreshold.toBigDecimal()

        if (hourChange > -buyThresholdBD) {
            return createDecision(TradeAction.SKIP, "条件に合致せず（1時間下落不足）")
        }

        if (!isReboundKline(latestKline)) {
            return createDecision(TradeAction.SKIP, "反発未確認")
        }

        if (!isTrendConfirm(last12Klines)) {
            return createDecision(TradeAction.SKIP, "MA5上抜け未確認")
        }

        val currentAtr = calculateAtr(recentKlines, config.atrLength)

        return TradeDecision(TradeAction.BUY_CANDIDATE, "1時間下落後の反発確認およびMA5上抜け確認", currentAtr)
    }

    /**
     * 売却（エグジット）の判定を行います。
     *
     * エントリー時に計算されたATR（entryAtr）と設定された倍率を用いて、
     * 動的に利確ラインおよび損切りラインを決定します。
     * 最新の終値がこれらのラインに到達している場合は売却候補として判定結果を返します。
     *
     * @param latestClose 最新の終値
     * @param buyPrice 現在の購入価格
     * @param entryAtr エントリー時に算出されたATR
     * @return 判定結果 (SELL_CANDIDATE または HOLDING)
     */
    private fun judgeExit(latestClose: BigDecimal, buyPrice: BigDecimal, entryAtr: BigDecimal?): TradeDecision {
        if (buyPrice <= BigDecimal.ZERO) return createDecision(TradeAction.HOLDING, "購入価格が未設定")
        if (entryAtr == null || entryAtr <= BigDecimal.ZERO) return createDecision(TradeAction.HOLDING, "エントリー時のATRが未設定または不正")

        val profitMultiplier = config.atrProfitMultiplier.toBigDecimal()
        val lossMultiplier = config.atrLossMultiplier.toBigDecimal()

        val takeProfitPrice = buyPrice + (entryAtr * profitMultiplier)
        if (latestClose >= takeProfitPrice) return createDecision(TradeAction.SELL_CANDIDATE, "利確 (ATR基準)")

        val stopLossPrice = buyPrice - (entryAtr * lossMultiplier)
        if (latestClose <= stopLossPrice) return createDecision(TradeAction.SELL_CANDIDATE, "損切り (ATR基準)")

        return createDecision(TradeAction.HOLDING, "条件に合致せず（保有継続）")
    }

    /**
     * 直近15分（K線3本分）で価格が急変動しているかを確認します。
     *
     * @param recentKlines 直近のK線データ
     * @return 急変動している場合は true、それ以外は false
     */
    private fun isSharpChange(recentKlines: List<Kline>): Boolean {
        val recent3Klines = recentKlines.takeLast(3)
        val maxHigh3 = recent3Klines.maxOfOrNull { it.high.toBigDecimal() } ?: BigDecimal.ZERO
        val minLow3 = recent3Klines.minOfOrNull { it.low.toBigDecimal() } ?: BigDecimal.ONE
        if (minLow3 <= BigDecimal.ZERO) return false
        val sharpChangeRate = (maxHigh3 - minLow3).divide(minLow3, 8, RoundingMode.HALF_UP)
        return sharpChangeRate >= config.sharpChangeThreshold.toBigDecimal()
    }

    /**
     * 直近1時間での価格の変動率を計算します。
     *
     * @param latestClose 最新の終値
     * @param oldestOpen 1時間前の始値
     * @return 1時間の価格変動率
     */
    private fun calculateHourChange(latestClose: BigDecimal, oldestOpen: BigDecimal): BigDecimal {
        return if (oldestOpen > BigDecimal.ZERO) {
            (latestClose - oldestOpen).divide(oldestOpen, 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * 最新のK線が反発のサインを示しているかを確認します。
     *
     * 陽線であるか、または下ヒゲが実体より長い場合は反発とみなします。
     *
     * @param latestKline 最新のK線データ
     * @return 反発のサインがある場合は true、それ以外は false
     */
    private fun isReboundKline(latestKline: Kline): Boolean {
        val latestOpen = latestKline.open.toBigDecimal()
        val latestClose = latestKline.close.toBigDecimal()
        val latestLow = latestKline.low.toBigDecimal()
        val isYang = latestClose > latestOpen
        val body = (latestClose - latestOpen).abs()
        val lowerWick = latestOpen.min(latestClose) - latestLow
        return isYang || lowerWick > body
    }

    /**
     * MA5（直近5本の終値平均）の上抜け確認を行います。
     *
     * @param recentKlines 直近のK線データ
     * @return MA5を上抜けている場合は true、それ以外は false
     */
    private fun isTrendConfirm(recentKlines: List<Kline>): Boolean {
        if (recentKlines.size < 6) return false
        val latest5Klines = recentKlines.takeLast(5)
        val latestMa5 = latest5Klines.map { it.close.toBigDecimal() }.reduce { acc, bigDecimal -> acc + bigDecimal }.divide(BigDecimal(5), 8, RoundingMode.HALF_UP)
        val previous5Klines = recentKlines.dropLast(1).takeLast(5)
        val previousMa5 = previous5Klines.map { it.close.toBigDecimal() }.reduce { acc, bigDecimal -> acc + bigDecimal }.divide(BigDecimal(5), 8, RoundingMode.HALF_UP)
        val latestClose = recentKlines.last().close.toBigDecimal()
        val previousClose = recentKlines[recentKlines.lastIndex - 1].close.toBigDecimal()
        return latestClose > latestMa5 && previousClose <= previousMa5 && latestMa5 > previousMa5
    }

    /**
     * ATR (Average True Range) を計算します。
     *
     * TR（True Range）は以下の3つのうちの最大値として計算されます。
     * 1. 当日の高値 - 当日の安値
     * 2. 当日の高値 - 前日の終値（の絶対値）
     * 3. 当日の安値 - 前日の終値（の絶対値）
     * ATRは指定された期間のTRの単純移動平均です。
     *
     * @param klines 計算に使用するK線データ（長さは length + 1 以上必要）
     * @param length ATRの計算期間
     * @return 計算されたATR
     */
    private fun calculateAtr(klines: List<Kline>, length: Int): BigDecimal {
        if (klines.size < length + 1) return BigDecimal.ZERO
        val targetKlines = klines.takeLast(length + 1)
        var sumTr = BigDecimal.ZERO
        for (i in 1..length) {
            val current = targetKlines[i]
            val previous = targetKlines[i - 1]
            val currentHigh = current.high.toBigDecimal()
            val currentLow = current.low.toBigDecimal()
            val previousClose = previous.close.toBigDecimal()
            val tr1 = currentHigh - currentLow
            val tr2 = (currentHigh - previousClose).abs()
            val tr3 = (currentLow - previousClose).abs()
            sumTr += tr1.max(tr2).max(tr3)
        }
        return sumTr.divide(BigDecimal(length), 8, RoundingMode.HALF_UP)
    }

    /**
     * 売買アクションと理由文字列をもとに、判定結果となる TradeDecision オブジェクトを生成します。
     *
     * @param action 決定した売買アクション
     * @param reason アクションを決定した理由
     * @return 生成された判定結果
     */
    private fun createDecision(action: TradeAction, reason: String): TradeDecision {
        val decision = TradeDecision(action, reason)
        logger.debug { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
