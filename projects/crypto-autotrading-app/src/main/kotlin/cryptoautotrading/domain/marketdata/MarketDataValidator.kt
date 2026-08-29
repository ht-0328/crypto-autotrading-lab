package cryptoautotrading.domain.marketdata

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.time.TradingTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.time.Clock

/**
 * 売買判定に使う市場データが信用できるかどうかを検証するサービス。
 *
 * 壊れたデータや古いデータでそのまま判定すると、誤った売買サインが出る。
 * 実注文では、それがそのまま誤発注になる。判定の前段でデータ自体を確認し、
 * 少しでも信用できない場合は判定を見送る。
 *
 * @property clock データの鮮度を判定するための時計
 */
class MarketDataValidator(
    private val clock: Clock = TradingTime.systemClock()
) {

    private val logger = KotlinLogging.logger {}

    /**
     * K線データが売買判定に使えるかどうかを検証する。
     *
     * @param klines 取得したK線データ
     * @param interval K線の間隔（例: "5min"）
     * @return 検証結果。使えない場合は理由付きで不可を返す
     */
    fun validate(klines: List<Kline>, interval: String): MarketDataValidationResult {
        if (klines.isEmpty()) {
            return invalid("K線データが空です")
        }

        val intervalMillis = parseIntervalMillis(interval)
            ?: return invalid("K線の間隔 ($interval) を解釈できません")

        klines.forEach { kline ->
            validatePrices(kline)?.let { return it }
        }

        val openTimes = klines.map { it.openTime.toLongOrNull() ?: return invalid("開始時刻 (${it.openTime}) を数値として解釈できません") }

        validateNoDuplicates(openTimes)?.let { return it }

        val sortedOpenTimes = openTimes.sorted()
        validateContinuity(sortedOpenTimes, intervalMillis)?.let { return it }
        validateFreshness(sortedOpenTimes.last(), intervalMillis)?.let { return it }

        return MarketDataValidationResult(isValid = true)
    }

    /**
     * 1本のK線の価格が妥当かどうかを検証する。
     *
     * @param kline 検証するK線
     * @return 問題があれば不可の結果、問題なければ null
     */
    private fun validatePrices(kline: Kline): MarketDataValidationResult? {
        val open = kline.open.toBigDecimalOrNull() ?: return invalid("始値 (${kline.open}) を数値として解釈できません")
        val high = kline.high.toBigDecimalOrNull() ?: return invalid("高値 (${kline.high}) を数値として解釈できません")
        val low = kline.low.toBigDecimalOrNull() ?: return invalid("安値 (${kline.low}) を数値として解釈できません")
        val close = kline.close.toBigDecimalOrNull() ?: return invalid("終値 (${kline.close}) を数値として解釈できません")

        if (open <= BigDecimal.ZERO || high <= BigDecimal.ZERO || low <= BigDecimal.ZERO || close <= BigDecimal.ZERO) {
            return invalid("価格が0以下のK線があります。openTime=${kline.openTime}")
        }

        if (high < low) {
            return invalid("高値 ($high) が安値 ($low) を下回っています。openTime=${kline.openTime}")
        }

        if (high < open || high < close) {
            return invalid("高値 ($high) が始値や終値を下回っています。openTime=${kline.openTime}")
        }

        if (low > open || low > close) {
            return invalid("安値 ($low) が始値や終値を上回っています。openTime=${kline.openTime}")
        }

        return null
    }

    /**
     * 開始時刻に重複が無いかを検証する。
     *
     * 同じ時刻のK線が複数あると、本数を数える判定がずれる。
     *
     * @param openTimes 各K線の開始時刻
     * @return 重複があれば不可の結果、なければ null
     */
    private fun validateNoDuplicates(openTimes: List<Long>): MarketDataValidationResult? {
        if (openTimes.size != openTimes.distinct().size) {
            return invalid("開始時刻が重複しているK線があります")
        }
        return null
    }

    /**
     * K線が等間隔に並んでいるかを検証する。
     *
     * 途中が抜けていると、実際より短い期間で判定していることになる。
     *
     * @param sortedOpenTimes 昇順に並べた開始時刻
     * @param intervalMillis K線の間隔（ミリ秒）
     * @return 欠損があれば不可の結果、なければ null
     */
    private fun validateContinuity(
        sortedOpenTimes: List<Long>,
        intervalMillis: Long
    ): MarketDataValidationResult? {
        sortedOpenTimes.zipWithNext().forEach { (previous, next) ->
            val actualInterval = next - previous
            if (actualInterval != intervalMillis) {
                return invalid(
                    "K線の間隔が想定と異なります。想定=${intervalMillis}ms, 実際=${actualInterval}ms, " +
                        "openTime=$previous から $next"
                )
            }
        }
        return null
    }

    /**
     * 最新のK線が古すぎないかを検証する。
     *
     * 取引所の応答が遅れていたり、実行が止まっていたりすると、古い価格で判定してしまう。
     *
     * @param latestOpenTime 最新のK線の開始時刻
     * @param intervalMillis K線の間隔（ミリ秒）
     * @return 古すぎれば不可の結果、問題なければ null
     */
    private fun validateFreshness(latestOpenTime: Long, intervalMillis: Long): MarketDataValidationResult? {
        val nowMillis = clock.instant().toEpochMilli()
        val elapsedMillis = nowMillis - latestOpenTime

        if (elapsedMillis < 0) {
            return invalid("最新のK線の開始時刻が未来です。openTime=$latestOpenTime, now=$nowMillis")
        }

        val allowedMillis = intervalMillis * ALLOWED_STALENESS_INTERVALS
        if (elapsedMillis > allowedMillis) {
            return invalid(
                "最新のK線が古すぎます。経過=${elapsedMillis}ms, 許容=${allowedMillis}ms, " +
                    "openTime=$latestOpenTime"
            )
        }

        return null
    }

    /**
     * K線の間隔の文字列をミリ秒に変換する。
     *
     * @param interval 間隔の文字列（例: "5min", "1hour", "1day"）
     * @return ミリ秒。解釈できない場合は null
     */
    private fun parseIntervalMillis(interval: String): Long? {
        val match = INTERVAL_PATTERN.matchEntire(interval.trim()) ?: return null
        val amount = match.groupValues[1].toLongOrNull() ?: return null
        if (amount <= 0) {
            return null
        }

        return when (match.groupValues[2]) {
            "min" -> amount * MILLIS_PER_MINUTE
            "hour" -> amount * MILLIS_PER_MINUTE * MINUTES_PER_HOUR
            "day" -> amount * MILLIS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY
            else -> null
        }
    }

    /**
     * 検証に失敗した結果を作る。
     *
     * @param reason 使えない理由
     * @return 不可を表す結果
     */
    private fun invalid(reason: String): MarketDataValidationResult {
        logger.warn { "市場データの検証NG: $reason" }
        return MarketDataValidationResult(isValid = false, reason = reason)
    }

    private companion object {
        /** 間隔の文字列を「数値」と「単位」に分けるパターン */
        val INTERVAL_PATTERN = Regex("^(\\d+)(min|hour|day)$")

        /** 1分のミリ秒 */
        const val MILLIS_PER_MINUTE = 60_000L

        /** 1時間の分数 */
        const val MINUTES_PER_HOUR = 60L

        /** 1日の時間数 */
        const val HOURS_PER_DAY = 24L

        /**
         * 最新のK線が古いと判断するまでに許容する間隔の本数。
         * 取引所の応答の遅れを見込んで、間隔の3本分までは許容する。
         */
        const val ALLOWED_STALENESS_INTERVALS = 3L
    }
}
