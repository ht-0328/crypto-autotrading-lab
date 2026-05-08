package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AtrTrendConfirmReboundStrategyTest {

    private val defaultConfig = TradingConfig(
        strategyName = "AtrTrendConfirmReboundStrategy",
        symbol = "BTC",
        initialCapital = 10000,
        tradeAmount = 1000,
        buyThreshold = 0.05,
        sellThreshold = 0.05,
        volatilityThreshold = 0.003,
        sharpChangeThreshold = 0.5,
        cooldownLength = 12,
        atrLength = 14,
        atrProfitMultiplier = 2.0,
        atrLossMultiplier = 2.0
    )

    private fun createKline(openTime: String, open: String, high: String, low: String, close: String): Kline {
        return Kline(openTime, open, high, low, close, "10.0")
    }

    @Test
    fun `買い条件を満たす場合、BUY_CANDIDATEとなりATRが算出されること`() {
        val strategy = AtrTrendConfirmReboundStrategy(defaultConfig)

        val klines = (1..15).map { i ->
            when (i) {
                1 -> createKline(String.format("%02d", i), "100", "100", "100", "100")
                in 2..10 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                11 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                12 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                13 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                14 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                15 -> createKline(String.format("%02d", i), "90", "95", "90", "91")
                else -> createKline(String.format("%02d", i), "90", "90", "90", "90")
            }
        }

        // Klines from index 4 to 15 are the "recentKlines"
        // oldestOpen (index 4) = 90
        // latestClose (index 15) = 91
        // hourChange = (91 - 90)/90 = 1/90 = +0.011...
        // buyThresholdBD is 0.05. Is 0.011 > -0.05 ? YES.
        // Thus it will skip due to "条件に合致せず（1時間下落不足）"

        // Let's fix the test so hourChange <= -0.05.
        // oldestOpen needs to be high enough.
        val klinesFixed = (1..15).map { i ->
            when (i) {
                1, 2, 3 -> createKline(String.format("%02d", i), "100", "100", "100", "100")
                4 -> createKline(String.format("%02d", i), "100", "100", "100", "100") // index 3 (oldest) is 100
                in 5..10 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                11, 12, 13, 14 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                15 -> createKline(String.format("%02d", i), "90", "95", "90", "91") // latest close = 91
                else -> createKline(String.format("%02d", i), "90", "90", "90", "90")
            }
        }

        val decision = strategy.judge(klinesFixed, SimulationState(isHolding = false, lastStopLossTime = ""))

        assertEquals(TradeAction.BUY_CANDIDATE, decision.action, decision.reason)
        assertNotNull(decision.atr)
    }

    @Test
    fun `保有中で利確ラインを超えた場合SELL_CANDIDATEとなること`() {
        val strategy = AtrTrendConfirmReboundStrategy(defaultConfig)
        val klines = (1..15).map { createKline(String.format("%02d", it), "100", "125", "100", "121") }
        val state = SimulationState(isHolding = true, buyPrice = BigDecimal("100"), entryAtr = BigDecimal("10.0"))
        val decision = strategy.judge(klines, state)
        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
    }

    @Test
    fun `保有中で損切りラインを下回った場合SELL_CANDIDATEとなること`() {
        val strategy = AtrTrendConfirmReboundStrategy(defaultConfig)
        val klines = (1..15).map { createKline(String.format("%02d", it), "100", "100", "75", "79") }
        val state = SimulationState(isHolding = true, buyPrice = BigDecimal("100"), entryAtr = BigDecimal("10.0"))
        val decision = strategy.judge(klines, state)
        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
    }

    @Test
    fun `保有中で利確・損切りラインに達していない場合はHOLDINGとなること`() {
        val strategy = AtrTrendConfirmReboundStrategy(defaultConfig)
        val klines = (1..15).map { createKline(String.format("%02d", it), "100", "110", "90", "105") }
        val state = SimulationState(isHolding = true, buyPrice = BigDecimal("100"), entryAtr = BigDecimal("10.0"))
        val decision = strategy.judge(klines, state)
        assertEquals(TradeAction.HOLDING, decision.action)
    }
}
