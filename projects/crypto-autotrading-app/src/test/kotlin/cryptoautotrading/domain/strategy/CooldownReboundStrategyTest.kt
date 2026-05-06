package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CooldownReboundStrategyTest {

    private val defaultConfig = TradingConfig(
        strategyName = "CooldownReboundStrategy",
        symbol = "BTC",
        initialCapital = 10000,
        tradeAmount = 1000,
        buyThreshold = 0.05,
        sellThreshold = 0.05,
        volatilityThreshold = 0.003,
        sharpChangeThreshold = 0.01,
        cooldownLength = 12
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
    fun `クールダウン期間中の場合は買い条件を満たしていてもSKIPを返すこと`() {
        val strategy = CooldownReboundStrategy(defaultConfig)

        // Setup klines where a buy would normally be triggered
        val klines = (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                // Yang line
                createKline(String.format("%02d", i), "90", "90.5", "90", "90.5")
            } else {
                createKline(String.format("%02d", i), "90.2", "90.2", "90.2", "90.2")
            }
        }

        // Set lastStopLossTime to "10", meaning it happened 3 klines ago (12 - 10 + 1 = 3 <= 12)
        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = "10"))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("クールダウン期間中"))
    }

    @Test
    fun `クールダウン期間を過ぎた場合は買い条件を満たしていればBUY_CANDIDATEを返すこと`() {
        val strategy = CooldownReboundStrategy(defaultConfig.copy(cooldownLength = 2)) // Short cooldown for test

        val klines = (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                createKline(String.format("%02d", i), "90", "90.5", "90", "90.5")
            } else {
                createKline(String.format("%02d", i), "90.2", "90.2", "90.2", "90.2")
            }
        }

        // Set lastStopLossTime to "09", meaning it happened 4 klines ago (12 - 9 + 1 = 4 > 2)
        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = "09"))

        assertEquals(TradeAction.BUY_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("1時間下落後の反発確認"))
    }

    @Test
    fun `損切りが発生した場合はSELL_CANDIDATEかつ理由が損切りとなること`() {
        val strategy = CooldownReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            createKline(String.format("%02d", i), "100", "100", "100", "90") // 10% drop
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = true, buyPrice = BigDecimal("100")))

        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("損切り"))
    }
}
