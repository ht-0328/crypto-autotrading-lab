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
 * 損切り後に一定期間エントリーを見送るクールダウン期間を設けた反発狙い戦略。
 *
 * SafeReboundStrategy と同様に、直近12本（5分足なら約1時間）のK線を使用して反発買いを狙います。
 * 非保有時は、直近の急変動がなく、1時間で一定以上下落しており、かつ最新のK線で反発サインが確認できた場合に買い候補（BUY_CANDIDATE）とします。
 * 保有時は、buyPrice を基準に利確ライン（sellThreshold）と損切りライン（sellThreshold）に達しているかを確認し、到達していれば売り候補（SELL_CANDIDATE）とします。
 * 損切りとなった場合は、そこから `config.cooldownLength` 本分のK線の間は、買い条件を満たしても買わずに見送り（SKIP）とします。
 * cooldownLength の初期値は12で、5分足なら約1時間のクールダウンを意味します。
 *
 * @property config 取引設定
 */
class CooldownReboundStrategy(
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
     *
     * @param klines K線データのリスト
     * @param currentState 現在のシミュレーション状態
     * @return 判定結果
     */
    override fun judge(klines: List<Kline>, currentState: SimulationState): TradeDecision {
        val isHolding = currentState.isHolding
        val buyPrice = currentState.buyPrice

        logger.debug { "売買判定を開始します (CooldownReboundStrategy)" }
        logger.debug { "入力値: K線データ件数=${klines.size}, 保有状態=$isHolding, 購入価格=$buyPrice, 最終損切り時刻=${currentState.lastStopLossTime}" }

        // 直近12本のデータのみを使用（1時間が対象）
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
     * 具体例 (`config.cooldownLength` = 12 の場合):
     * - 損切りしたK線の index = 100
     * - 現在のK線 index = 100 の場合、損切りと同じK線なので true
     * - 現在のK線 index = 101 の場合、損切り後1本目なので true
     * - 現在のK線 index = 112 の場合、損切り後12本目なので true
     * - 現在のK線 index = 113 の場合、損切り後13本目なので false
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

        // 損切りが発生したK線のインデックスを探す
        val stopLossIndex = sortedKlines.indexOfLast { it.openTime == lastStopLossTime }

        if (stopLossIndex == -1) {
            // 損切りした時刻のK線が見つからない場合はクールダウン期間外とする（仕様に依存するが安全側に倒す）
            return false
        }

        val currentIndex = sortedKlines.lastIndex
        val elapsedKlineCount = currentIndex - stopLossIndex

        // The current kline might have the exact same time as the stop loss
        // e.g. if we are evaluating during the same K-line period where the sell happened.
        // Or it might be missing from the list.
        if (elapsedKlineCount < 0) return false

        // 損切りした次の足(差分1)〜指定本数(差分12)まではクールダウン期間とする
        return elapsedKlineCount <= config.cooldownLength
    }

    /**
     * 新規購入（エントリー）の判定を行います。
     * 非保有で、かつクールダウン期間外の場合に呼ばれます。
     * 直近12本のK線データを使用します。
     *
     * 最初に `isSharpChange` を呼び出し、直近3本のK線で急変動がないかを確認します。
     * 急変動率が `config.sharpChangeThreshold` 以上であれば SKIP を返します。
     * 次に、直近12本の最初の open と最新の close から1時間の価格変動率を計算します。
     * 変動率が `-config.buyThreshold` よりも大きい（下落が不足している）場合は SKIP を返します。
     * 変動率が `-config.buyThreshold` 以下の場合は、1時間で十分に下落したものとみなします。
     * 最後に、最新のK線が陽線であるか、または下ヒゲが実体より長ければ反発サインとみなします。
     * 反発サインがあれば BUY_CANDIDATE を返し、なければ SKIP を返します。
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

        return createDecision(TradeAction.BUY_CANDIDATE, "1時間下落後の反発確認")
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
     * 損切りの場合、呼び出し元の SimulationService 側で `lastStopLossTime` が記録され、以後のクールダウン期間が開始されます。
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

        // 利確: latestClose >= buyPrice * (1 + config.sellThreshold)
        val takeProfitPrice = buyPrice * (BigDecimal.ONE + sellThresholdBD)
        if (latestClose >= takeProfitPrice) {
            return createDecision(TradeAction.SELL_CANDIDATE, "利確")
        }

        // 損切り: latestClose <= buyPrice * (1 - config.sellThreshold)
        val stopLossPrice = buyPrice * (BigDecimal.ONE - sellThresholdBD)
        if (latestClose <= stopLossPrice) {
            // 損切りになった場合、SimulationServiceでlastStopLossTimeが更新されクールダウンが始まる
            return createDecision(TradeAction.SELL_CANDIDATE, "損切り")
        }

        return createDecision(TradeAction.HOLDING, "条件に合致せず（保有継続）")
    }

    /**
     * 直近15分（K線3本分）で価格が急変動しているかを確認します。
     *
     * 直近3本のK線から、high の最大値 (`maxHigh3`) と low の最小値 (`minLow3`) を取得します。
     * `minLow3` が 0 以下の場合は割り算ができないため、急変動なしとして false を返します。
     * 急変動率を `(maxHigh3 - minLow3) / minLow3` で計算します。
     * 計算した急変動率が `config.sharpChangeThreshold` 以上であれば true を返します。
     * `config.sharpChangeThreshold` 未満であれば false を返します。
     * true を返した場合、呼び出し元の `judgeEntry` 側で買い見送り（SKIP）の判定となります。
     *
     * @param recentKlines 直近12本のK線データ（内部で最後の3本を使用）
     * @return 急変動している場合は true、それ以外は false
     */
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

    /**
     * 直近1時間での価格の変動率を計算します。
     *
     * 直近12本の最初の始値 (`oldestOpen`) と最新の終値 (`latestClose`) を使用します。
     * 価格変動率を `(latestClose - oldestOpen) / oldestOpen` で計算します。
     * `oldestOpen` が 0 以下の場合は割り算ができないため、変動なしとして `BigDecimal.ZERO` を返します。
     * 戻り値がマイナスの場合は、1時間で価格が下落していることを意味します。
     * 戻り値がプラスの場合は、1時間で価格が上昇していることを意味します。
     * 呼び出し元の `judgeEntry` では、この値が `-config.buyThreshold` 以下かどうかを確認し、十分に下落したかを判断します。
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
     * 最新のK線1本だけを判定対象とします。
     * `latestClose > latestOpen` であれば陽線であり、反発サインとみなします。
     * 実体は `abs(latestClose - latestOpen)` で計算します。
     * 下ヒゲは `min(latestOpen, latestClose) - latestLow` で計算します。
     * 下ヒゲが実体より長い場合は、ピンバー（下ヒゲの長いローソク足）として反発サインとみなします。
     * 陽線であるか、または下ヒゲが実体より長い場合は true を返します。
     * どちらでもない場合は false を返します。
     * true を返した場合、呼び出し元の `judgeEntry` 側で買い候補（BUY_CANDIDATE）の条件を満たすことになります。
     *
     * @param latestKline 最新のK線データ
     * @return 反発のサインがある場合は true、それ以外は false
     */
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

    /**
     * 売買アクションと理由文字列をもとに、判定結果となる `TradeDecision` オブジェクトを生成します。
     *
     * 生成された `TradeDecision` の内容を debug ログに出力します。
     * 売買条件そのものの判断はここでは行いません。
     *
     * @param action 決定した売買アクション (BUY_CANDIDATE, SELL_CANDIDATE, SKIP, HOLDING)
     * @param reason アクションを決定した理由
     * @return 生成された判定結果
     */
    private fun createDecision(action: TradeAction, reason: String): TradeDecision {
        val decision = TradeDecision(action, reason)
        logger.debug { "売買判定結果: ${decision.action.description} (理由: ${decision.reason})" }
        return decision
    }
}
