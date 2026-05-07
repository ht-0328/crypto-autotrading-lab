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
 * 損切り後のクールダウン期間に加え、短期的なトレンド転換（MA5の上抜け）を確認してから買いを行う反発狙い戦略。
 *
 * CooldownReboundStrategy の課題であった「早すぎる買い（下落トレンド中のダマシの反発を拾ってしまう問題）」を解決するため、
 * 単発の反発サインだけでなく、短期トレンド（MA5：直近5本の終値平均）が上向きに変わり始めたことをエントリー条件に追加しています。
 * 非保有時は、急変動がなく、1時間で一定以上下落し、反発サインがあり、かつMA5の上抜けが確認できた場合に買い候補とします。
 * 保有時の利確・損切りライン、および損切り後のクールダウン期間の仕様は CooldownReboundStrategy と同一です。
 * MA5の計算に必要なK線本数を確保するため、最低でも直近6本のデータが必要です。
 *
 * @property config 取引設定
 */
class TrendConfirmReboundStrategy(
    private val config: TradingConfig
) : TradingStrategy {

    private val logger = KotlinLogging.logger {}

    /**
     * K線データと現在のシミュレーション状態から売買判定を行います。
     *
     * klines を openTime 順に並べ替え、直近12本のK線を使用します。
     * 直近12本未満の場合、データ不足として、保有中なら HOLDING を、非保有なら SKIP を返します。
     * 保有中の場合はクールダウン判定をせず、`judgeExit` で売却判定を行います。
     * 非保有の場合は、まず `isCooldownPeriod` を呼び出してクールダウン期間中かどうかを確認します。
     * クールダウン期間中であれば、買い条件を確認せずに SKIP を返します。
     * クールダウン期間外であれば、`judgeEntry` を呼び出して買い判定を行います。
     * MA5計算のため、最新のK線と直前のK線それぞれの過去5本分、計6本以上のデータが内部で利用されます。
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
     * 現在が損切り直後のクールダウン期間中かどうかを判定します。
     *
     * `lastStopLossTime` には、最後に損切りしたK線の openTime が格納されています。
     * `lastStopLossTime` が空の場合は、過去に損切りしていないため false を返します。
     * klines を openTime 順に並べ替え、`lastStopLossTime` と一致するK線を「損切りしたK線」としてインデックスを探します。
     * 一致するK線が見つからない場合は、損切り位置を特定できないため安全側に倒して false を返します。
     * 現在の判定対象は、並べ替え後の最後のK線（`lastIndex`）です。
     * 損切りしたK線のインデックスと現在のK線のインデックスの差分を `elapsedKlineCount` とします。
     * `elapsedKlineCount` が 1 の場合は、損切り後1本目であることを意味します。
     * `elapsedKlineCount` が 0 未満の場合は未来の時刻となるため false を返します。
     * `config.cooldownLength`（例：12）の場合、`elapsedKlineCount` が 1〜12 ならクールダウン期間中で true を返します。
     * `elapsedKlineCount` が 13 以上ならクールダウン期間外となり false を返します。
     * `elapsedKlineCount` が 0 の場合（損切りと同じK線期間中）も、まだクールダウン期間を経過していないため true を返します。
     *
     * @param klines K線データ全体
     * @param lastStopLossTime 最後に損切りした時刻
     * @return クールダウン期間中の場合は true、それ以外は false
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
     * 新規購入（エントリー）の判定を行います。
     * 非保有で、かつクールダウン期間外の場合に呼ばれます。
     * 直近12本のK線データを使用します。
     *
     * 最初に直近3本のK線で急変動がないかを確認します。急変動があれば SKIP を返します。
     * 次に、直近1時間の価格変動率を計算し、十分に下落していない場合は SKIP を返します。
     * 次に、最新のK線が反発サイン（陽線または長い下ヒゲ）を示しているか確認し、なければ SKIP を返します。
     * 最後に、短期トレンドが上向きに変わり始めたかを確認するため、MA5上抜け判定を行います。上抜けがなければ SKIP を返します。
     * 全ての条件を満たした場合に BUY_CANDIDATE を返します。
     *
     * @param recentKlines 直近12本のK線データ
     * @return 判定結果 (BUY_CANDIDATE または SKIP)
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
     * 売却（エグジット）の判定を行います。
     * 保有中の場合に呼ばれます。
     *
     * `buyPrice` が 0 以下であれば、購入価格が不正または未設定のため HOLDING を返します。
     * `config.sellThreshold` を使用して、利確ラインと損切りラインを計算します。
     * 利確ラインは `buyPrice * (1 + config.sellThreshold)` となります。
     * 損切りラインは `buyPrice * (1 - config.sellThreshold)` となります。
     * `latestClose` が利確ライン以上であれば、SELL_CANDIDATE を返し、理由を「利確」とします。
     * `latestClose` が損切りライン以下であれば、SELL_CANDIDATE を返し、理由を「損切り」とします。
     * どちらにも該当しない場合は HOLDING を返します。
     *
     * @param latestClose 最新の終値
     * @param buyPrice 現在の購入価格
     * @return 判定結果 (SELL_CANDIDATE または HOLDING)
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
     * 直近15分（K線3本分）で価格が急変動しているかを確認します。
     *
     * 直近3本のK線から、high の最大値 (`maxHigh3`) と low の最小値 (`minLow3`) を取得します。
     * 急変動率を `(maxHigh3 - minLow3) / minLow3` で計算し、
     * `config.sharpChangeThreshold` 以上であれば true を返します。
     *
     * @param recentKlines 直近12本のK線データ（内部で最後の3本を使用）
     * @return 急変動している場合は true、それ以外は false
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
     * 直近1時間での価格の変動率を計算します。
     *
     * 直近12本の最初の始値 (`oldestOpen`) と最新の終値 (`latestClose`) を使用します。
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
     * 陽線であるか、または下ヒゲが実体より長い場合は true を返します。
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
        val hasLongLowerWick = lowerWick > body

        return isYang || hasLongLowerWick
    }

    /**
     * MA5（直近5本の終値平均）の上抜け確認を行う。
     * - 最新の終値が最新の MA5 より上
     * - 直前の終値は直前の MA5 以下
     * - 最新の MA5 が直前の MA5 より上
     *
     * この条件により、直前までは短期平均線の下にいたが、
     * 最新足で短期平均線を上抜け、MA5自体も上向きになっていることを確認します。
     * つまり、単なる一時的な反発ではなく、短期的な流れが上向きに変わり始めた可能性を判定します。
     *
     * @param recentKlines 直近12本のK線データ
     * @return MA5を上抜けている場合は true、それ以外は false
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
     * 売買アクションと理由文字列をもとに、判定結果となる `TradeDecision` オブジェクトを生成します。
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
