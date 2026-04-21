package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TradingStrategyTest {

    private val defaultConfig = TradingConfig(
        symbol = "BTC",
        initialCapital = 10000,
        tradeAmount = 1000,
        buyThreshold = 0.005,
        sellThreshold = 0.005,
        volatilityThreshold = 0.003,
        sharpChangeThreshold = 0.01
    )

    private fun createKline(openTime: String, open: String, high: String, low: String, close: String): Kline {
        return Kline(
            openTime = openTime,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = "10.0"
        )
    }

    @Test
    fun `judge should return SKIP or HOLDING due to insufficient data`() {
        // Arrange
        val strategy = TradingStrategy(defaultConfig)
        val klines = listOf(createKline("1", "100", "110", "90", "100"))

        // Act
        val decisionNotHolding = strategy.judge(klines, isHolding = false)
        val decisionHolding = strategy.judge(klines, isHolding = true)

        // Assert
        assertEquals(TradeAction.SKIP, decisionNotHolding.action)
        assertEquals(TradeAction.HOLDING, decisionHolding.action)
        assertEquals("データ不足（12本未満）", decisionNotHolding.reason)
    }

    @Test
    fun `judge should return SKIP or HOLDING due to low volatility`() {
        // Arrange
        val strategy = TradingStrategy(defaultConfig)
        // 12 klines with very low volatility (high-low variation < 0.3%)
        val klines = (1..12).map {
            createKline(String.format("%02d", it), "1000", "1001", "1000", "1001")
        }

        // Act
        val decisionNotHolding = strategy.judge(klines, isHolding = false)
        val decisionHolding = strategy.judge(klines, isHolding = true)

        // Assert
        assertEquals(TradeAction.SKIP, decisionNotHolding.action)
        assertEquals(TradeAction.HOLDING, decisionHolding.action)
        assertEquals("直近1時間の変動が 0.3%未満", decisionNotHolding.reason)
    }

    @Test
    fun `judge should return SKIP or HOLDING due to sharp drop in last 15 mins`() {
        // Arrange
        val strategy = TradingStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i == 12) {
                createKline(String.format("%02d", i), "980", "1005", "900", "980")
            } else if (i >= 10) {
                createKline(String.format("%02d", i), "1000", "1005", "900", "980")
            } else {
                createKline(String.format("%02d", i), "1000", "1005", "900", "1000")
            }
        }

        // Act
        val decisionNotHolding = strategy.judge(klines, isHolding = false)
        val decisionHolding = strategy.judge(klines, isHolding = true)

        // Assert
        assertEquals(TradeAction.SKIP, decisionNotHolding.action)
        assertEquals(TradeAction.HOLDING, decisionHolding.action)
        assertEquals("直近15分で 1.0%以上下落", decisionNotHolding.reason)
    }

    @Test
    fun `judge should return SKIP or HOLDING due to sharp rise in last 15 mins`() {
        // Arrange
        val strategy = TradingStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i == 12) {
                createKline(String.format("%02d", i), "1020", "1050", "950", "1020")
            } else if (i >= 10) {
                createKline(String.format("%02d", i), "1000", "1050", "950", "1020")
            } else {
                createKline(String.format("%02d", i), "1000", "1050", "950", "1000")
            }
        }

        // Act
        val decisionNotHolding = strategy.judge(klines, isHolding = false)
        val decisionHolding = strategy.judge(klines, isHolding = true)

        // Assert
        assertEquals(TradeAction.SKIP, decisionNotHolding.action)
        assertEquals(TradeAction.HOLDING, decisionHolding.action)
        assertEquals("直近15分で 1.0%以上上昇", decisionNotHolding.reason)
    }

    @Test
    fun `judge should return BUY_CANDIDATE when not holding and dropped more than 0_5 percent`() {
        // Arrange
        val strategy = TradingStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            // start at 1000, slowly drop to 994 (0.6% drop over 1 hour)
            // ensuring volatility >= 0.3% (high=1000, low=994 -> 0.6%)
            // ensuring 15m change is not sharp: last 3 klines are around 994
            createKline(String.format("%02d", i), "1000", "1000", "994", "994")
        }

        // Act
        val decision = strategy.judge(klines, isHolding = false)

        // Assert
        assertEquals(TradeAction.BUY_CANDIDATE, decision.action)
        assertEquals("0.5%下落", decision.reason)
    }

    @Test
    fun `judge should return SELL_CANDIDATE when holding and rose more than 0_5 percent`() {
        // Arrange
        val strategy = TradingStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            // start at 1000, slowly rise to 1006 (0.6% rise over 1 hour)
            createKline(String.format("%02d", i), "1000", "1006", "1000", "1006")
        }

        // Act
        val decision = strategy.judge(klines, isHolding = true)

        // Assert
        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
        assertEquals("0.5%上昇", decision.reason)
    }
}
