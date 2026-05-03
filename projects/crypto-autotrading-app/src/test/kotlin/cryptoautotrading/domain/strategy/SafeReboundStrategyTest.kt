package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SafeReboundStrategyTest {

    private val defaultConfig = TradingConfig(
        strategyName = "SafeReboundStrategy",
        symbol = "BTC",
        initialCapital = 10000,
        tradeAmount = 1000,
        buyThreshold = 0.05, // 5% drop
        sellThreshold = 0.05, // 5% profit/loss
        volatilityThreshold = 0.003,
        sharpChangeThreshold = 0.01 // 1%
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
    fun `K線データが12本未満の場合、未保有ならSKIPを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = listOf(createKline("1", "100", "110", "90", "100"))

        val decision = strategy.judge(klines, SimulationState(isHolding = false))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("データ不足"))
    }

    @Test
    fun `K線データが12本未満の場合、保有中ならHOLDINGを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = listOf(createKline("1", "100", "110", "90", "100"))

        val decision = strategy.judge(klines, SimulationState(isHolding = true))

        assertEquals(TradeAction.HOLDING, decision.action)
        assertTrue(decision.reason.contains("データ不足"))
    }

    @Test
    fun `直近3本の急変動がsharpChangeThreshold以上ならSKIPを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i >= 10) {
                // Latest 3 klines have a sharp drop (100 -> 90 is 10% which > 1%)
                createKline(String.format("%02d", i), "100", "100", "90", "95")
            } else {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("急変動"))
    }

    @Test
    fun `保有中でlatestCloseが損切りライン以下、かつ直近3本が急変動に該当する場合でも、SELL_CANDIDATEかつ理由損切りになること`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i >= 10) {
                // Sharp drop causing sharp change filter to hit AND stop loss to hit
                createKline(String.format("%02d", i), "100", "100", "90", "90")
            } else {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            }
        }

        // buyPrice 100, sellThreshold 0.05 -> stop loss at 95, latestClose is 90
        val decision = strategy.judge(klines, SimulationState(isHolding = true, buyPrice = BigDecimal("100")))

        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("損切り"))
    }

    @Test
    fun `保有中でlatestCloseが利確ライン以上、かつ直近3本が急変動に該当する場合でも、SELL_CANDIDATEかつ理由利確になること`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i >= 10) {
                // Sharp rise causing sharp change filter to hit AND take profit to hit
                createKline(String.format("%02d", i), "100", "115", "100", "110")
            } else {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            }
        }

        // buyPrice 100, sellThreshold 0.05 -> take profit at 105, latestClose is 110
        val decision = strategy.judge(klines, SimulationState(isHolding = true, buyPrice = BigDecimal("100")))

        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("利確"))
    }

    @Test
    fun `直近1時間で下落していても、最新足が陰線かつ下ヒゲが短い場合はSKIPを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                // Latest kline: Yin line (close < open), short lower wick
                // open=90, close=89.5, low=89.3 (body=0.5, wick=0.2)
                createKline(String.format("%02d", i), "90", "90", "89.3", "89.5") // High=90, Low=89.3
            } else {
                createKline(String.format("%02d", i), "90", "90", "89.3", "90") // Prevent sharpChangeThreshold skip
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("反発未確認"))
    }

    @Test
    fun `直近1時間で下落し、最新足が陽線ならBUY_CANDIDATEを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                // Latest kline: Yang line (close > open)
                // open=90, close=90.5, low=90
                createKline(String.format("%02d", i), "90", "90.5", "90", "90.5") // High=90.5, Low=90
            } else {
                createKline(String.format("%02d", i), "90.2", "90.2", "90.2", "90.2") // Prevent sharpChangeThreshold skip
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false))

        assertEquals(TradeAction.BUY_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("1時間下落後の反発確認"))
    }

    @Test
    fun `直近1時間で下落し、最新足の下ヒゲが実体より長ければBUY_CANDIDATEを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                // Latest kline: Yin line but long lower wick
                // open=90, close=89.8, low=89.2
                // body = 0.2, lower wick = 89.8 - 89.2 = 0.6 (longer than body)
                createKline(String.format("%02d", i), "90", "90", "89.2", "89.8")
            } else {
                createKline(String.format("%02d", i), "90", "90", "89.2", "90") // Prevent sharpChangeThreshold skip
            }
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = false))

        assertEquals(TradeAction.BUY_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("1時間下落後の反発確認"))
    }

    @Test
    fun `保有中にlatestCloseがbuyPriceの1+sellThreshold以上ならSELL_CANDIDATEを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            createKline(String.format("%02d", i), "100", "100", "100", "105") // 100 -> 105 (5% profit)
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = true, buyPrice = BigDecimal("100")))

        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("利確"))
    }

    @Test
    fun `保有中にlatestCloseがbuyPriceの1-sellThreshold以下ならSELL_CANDIDATEを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            createKline(String.format("%02d", i), "100", "100", "100", "95") // 100 -> 95 (5% loss)
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = true, buyPrice = BigDecimal("100")))

        assertEquals(TradeAction.SELL_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("損切り"))
    }

    @Test
    fun `保有中かつbuyPriceが0以下の場合はHOLDINGを返すこと`() {
        val strategy = SafeReboundStrategy(defaultConfig)
        val klines = (1..12).map { i ->
            createKline(String.format("%02d", i), "100", "100", "100", "100")
        }

        val decision = strategy.judge(klines, SimulationState(isHolding = true, buyPrice = BigDecimal("0")))

        assertEquals(TradeAction.HOLDING, decision.action)
        assertTrue(decision.reason.contains("購入価格が未設定"))
    }
}
