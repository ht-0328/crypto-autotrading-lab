package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TrendConfirmReboundStrategyTest {

    private val defaultConfig = TradingConfig(
        strategyName = "TrendConfirmReboundStrategy",
        symbol = "BTC",
        initialCapital = 10000,
        tradeAmount = 1000,
        buyThreshold = 0.05,
        sellThreshold = 0.05,
        volatilityThreshold = 0.003,
        sharpChangeThreshold = 0.5, // To pass sharp change threshold
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
    fun `MA5上抜け条件を満たさない場合はSKIPになること`() {
        val strategy = TrendConfirmReboundStrategy(defaultConfig)

        // MA5を満たさないようにデータを設定
        val klines = (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                // 下落は満たすし、陽線にもなるが、MA5よりは下になるようにする
                // 100 * (1 - 0.05) = 95 の条件
                createKline(String.format("%02d", i), "90", "91", "90", "91")
            } else {
                val price = 95 - (12 - i) * 0.1
                createKline(String.format("%02d", i), price.toString(), price.toString(), price.toString(), price.toString())
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = ""))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("MA5上抜け未確認"))
    }

    @Test
    fun `MA5上抜け条件を含む全ての買い条件を満たす場合はBUY_CANDIDATEになること`() {
        val strategy = TrendConfirmReboundStrategy(defaultConfig)

        val klines = (1..12).map { i ->
            when (i) {
                1 -> createKline(String.format("%02d", i), "100", "100", "100", "100")
                in 2..7 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                // previous MA5 (8..11) + previousClose(11) : prices are low
                8 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                9 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                10 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                11 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                // latest (12): jumps up, pulls up latest MA5
                12 -> createKline(String.format("%02d", i), "90", "95", "90", "91") // 陽線、MA5上抜け
                else -> createKline(String.format("%02d", i), "90", "90", "90", "90")
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = ""))

        assertEquals(TradeAction.BUY_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("MA5上抜け確認"))
    }

    @Test
    fun `クールダウン期間中の場合はSKIPになること`() {
        val strategy = TrendConfirmReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i == 1) createKline(String.format("%02d", i), "100", "100", "100", "100")
            else createKline(String.format("%02d", i), "90", "90", "90", "90")
        }

        // 損切り時刻がK線のリスト内の最新の時刻
        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = "12"))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("クールダウン期間中"))
    }
}
