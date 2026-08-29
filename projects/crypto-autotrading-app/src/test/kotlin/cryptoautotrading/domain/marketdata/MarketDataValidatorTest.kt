package cryptoautotrading.domain.marketdata

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.time.TradingTime
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant

class MarketDataValidatorTest {

    /** 2026-01-01T00:00:00Z のエポックミリ秒 */
    private val baseOpenTime = 1767225600000L

    private val fiveMinutesMillis = 5 * 60 * 1000L

    private fun clockAt(millis: Long): Clock =
        Clock.fixed(Instant.ofEpochMilli(millis), TradingTime.ZONE)

    private fun kline(
        openTime: Long,
        open: String = "1000000",
        high: String = "1010000",
        low: String = "990000",
        close: String = "1005000"
    ) = Kline(
        openTime = openTime.toString(),
        open = open,
        high = high,
        low = low,
        close = close,
        volume = "1.0"
    )

    private fun validKlines(count: Int = 3): List<Kline> =
        (0 until count).map { kline(baseOpenTime + it * fiveMinutesMillis) }

    /** 最新の足の1本分あとを指す時計を持つ検証器を作る */
    private fun validatorAtLatest(count: Int = 3): MarketDataValidator {
        val latest = baseOpenTime + (count - 1) * fiveMinutesMillis
        return MarketDataValidator(clockAt(latest + fiveMinutesMillis))
    }

    @Test
    fun `正常なK線データは使えると判定されること`() {
        val result = validatorAtLatest().validate(validKlines(), "5min")

        assertTrue(result.isValid)
    }

    @Test
    fun `K線データが空の場合は使えないと判定されること`() {
        val result = validatorAtLatest().validate(emptyList(), "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `間隔の文字列を解釈できない場合は使えないと判定されること`() {
        val result = validatorAtLatest().validate(validKlines(), "unknown")

        assertFalse(result.isValid)
    }

    @Test
    fun `開始時刻が数値として解釈できない場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[1] = klines[1].copy(openTime = "2026-01-01T00:05:00Z")

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `価格が数値として解釈できない場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[1] = klines[1].copy(close = "N/A")

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `価格が0以下の場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[1] = klines[1].copy(open = "0", high = "0", low = "0", close = "0")

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `高値が安値を下回る場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[1] = klines[1].copy(high = "900000", low = "1000000", open = "950000", close = "950000")

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `高値が終値を下回る場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[1] = klines[1].copy(high = "1000000", close = "1005000")

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `安値が始値を上回る場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[1] = klines[1].copy(low = "1002000", open = "1000000")

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `開始時刻が重複している場合は使えないと判定されること`() {
        val klines = validKlines().toMutableList()
        klines[2] = klines[2].copy(openTime = klines[1].openTime)

        val result = validatorAtLatest().validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `K線が1本欠けている場合は使えないと判定されること`() {
        // 間隔が2倍になる状態を作る
        val klines = listOf(
            kline(baseOpenTime),
            kline(baseOpenTime + 2 * fiveMinutesMillis)
        )
        val validator = MarketDataValidator(clockAt(baseOpenTime + 3 * fiveMinutesMillis))

        val result = validator.validate(klines, "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `最新のK線が古すぎる場合は使えないと判定されること`() {
        val latest = baseOpenTime + 2 * fiveMinutesMillis
        // 許容は間隔の3本分なので、4本分経過させる
        val validator = MarketDataValidator(clockAt(latest + 4 * fiveMinutesMillis))

        val result = validator.validate(validKlines(), "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `最新のK線が許容範囲内の古さなら使えると判定されること`() {
        val latest = baseOpenTime + 2 * fiveMinutesMillis
        val validator = MarketDataValidator(clockAt(latest + 3 * fiveMinutesMillis))

        val result = validator.validate(validKlines(), "5min")

        assertTrue(result.isValid)
    }

    @Test
    fun `最新のK線の開始時刻が未来の場合は使えないと判定されること`() {
        val validator = MarketDataValidator(clockAt(baseOpenTime))

        val result = validator.validate(validKlines(), "5min")

        assertFalse(result.isValid)
    }

    @Test
    fun `1hour の間隔も解釈できること`() {
        val hourMillis = 60 * 60 * 1000L
        val klines = listOf(
            kline(baseOpenTime),
            kline(baseOpenTime + hourMillis)
        )
        val validator = MarketDataValidator(clockAt(baseOpenTime + hourMillis + 1000))

        val result = validator.validate(klines, "1hour")

        assertTrue(result.isValid)
    }
}
