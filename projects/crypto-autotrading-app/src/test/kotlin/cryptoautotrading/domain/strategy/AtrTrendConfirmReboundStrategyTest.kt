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

        // 1時間下落条件(5%以上)と反発、MA5上抜けをすべて満たすK線を生成
        val klines = (1..15).map { i ->
            when (i) {
                1, 2, 3 -> createKline(String.format("%02d", i), "100", "100", "100", "100")
                4 -> createKline(String.format("%02d", i), "100", "100", "100", "100") // 直近12本の古い始値 = 100
                in 5..10 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                11, 12, 13, 14 -> createKline(String.format("%02d", i), "90", "90", "90", "90")
                15 -> createKline(String.format("%02d", i), "90", "95", "90", "91") // 最新終値 = 91 (下落率9%で条件クリア、かつ反発およびMA5上抜け)
                else -> createKline(String.format("%02d", i), "90", "90", "90", "90")
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = ""))

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

    @Test
    fun `保有中だがentryAtrが未設定の場合は安全のためHOLDINGとなること`() {
        val strategy = AtrTrendConfirmReboundStrategy(defaultConfig)

        // どんなに価格が変動しても（例: 大幅下落）、entryAtrが存在しない場合は判定をスキップして保有継続する
        val klines = (1..15).map { createKline(String.format("%02d", it), "100", "100", "10", "10") }

        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("100"),
            entryAtr = null // ATRが未設定
        )

        val decision = strategy.judge(klines, state)

        assertEquals(TradeAction.HOLDING, decision.action)
        assertTrue(decision.reason.contains("エントリー時のATRが未設定または不正"))
    }
}
