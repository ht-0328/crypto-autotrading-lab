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
    fun `損切り直後の次のK線ではSKIPになること`() {
        val strategy = CooldownReboundStrategy(defaultConfig) // cooldownLength = 12
        val klines = createTestKlines()

        // 損切りが直前 (index=10, 最後の足はindex=11なので差分は1)
        val decision = strategy.judge(klines, SimulationState(isHolding = false, lastStopLossTime = "11"))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("クールダウン期間中"))
    }

    @Test
    fun `cooldownLength = 12 の場合、損切り後12本目まではSKIPになること`() {
        val strategy = CooldownReboundStrategy(defaultConfig) // cooldownLength = 12
        val klines = createTestKlines()

        // 損切りが12本前 (現在の足がindex=11, 損切りした足がインデックスより前にあり、12本前となる場合をシミュレート)
        // 今回のリストはサイズ12 (index 0~11)。lastStopLossTimeがリストの最初の要素(index=0)の場合は、差分は 11-0 = 11 になる。
        // もし差分12の状態を作るなら、テストデータを増やすか、lastStopLossTimeの指定を工夫する。
        // リストは13本あると仮定して、直近12本をjudgeに渡す仕様だが、isCooldownPeriodは渡されたklines全体を見る。
        // CooldownReboundStrategy は takeLast(12) しているため、渡されたklinesのサイズ13でテストする。
        val longKlines = (0..13).map { i ->
            if (i == 13) createKline(String.format("%02d", i), "90", "90.5", "90", "90.5")
            else createKline(String.format("%02d", i), "90.2", "90.2", "90.2", "90.2")
        }

        // current index = 13
        // stopLossIndex needs to be 1, so difference is 13 - 1 = 12 <= 12
        val decision = strategy.judge(longKlines, SimulationState(isHolding = false, lastStopLossTime = "01"))

        assertEquals(TradeAction.SKIP, decision.action)
        assertTrue(decision.reason.contains("クールダウン期間中"))
    }

    @Test
    fun `cooldownLength = 12 の場合、損切り後13本目から買い条件を満たせばBUY_CANDIDATEになること`() {
        val strategy = CooldownReboundStrategy(defaultConfig) // cooldownLength = 12

        // Create 15 K-lines
        val longKlines = (0..14).map { i ->
            if (i == 3) createKline(String.format("%02d", i), "100", "100", "100", "100") // Oldest for 1-hour drop within takeLast(12) => 14 - 12 + 1 = 3
            else if (i == 14) createKline(String.format("%02d", i), "90", "90.5", "90", "90.5") // Latest (Yang line, dropped from 100)
            else createKline(String.format("%02d", i), "90.2", "90.2", "90.2", "90.2") // Middle ones
        }

        // current index = 14
        // stopLossIndex needs to be 1, so difference is 14 - 1 = 13 > 12
        val decision = strategy.judge(longKlines, SimulationState(isHolding = false, lastStopLossTime = "01"))

        assertEquals(TradeAction.BUY_CANDIDATE, decision.action)
        assertTrue(decision.reason.contains("1時間下落後の反発確認"))
    }

    private fun createTestKlines(): List<Kline> {
        return (1..12).map { i ->
            if (i == 1) {
                createKline(String.format("%02d", i), "100", "100", "100", "100")
            } else if (i == 12) {
                createKline(String.format("%02d", i), "90", "90.5", "90", "90.5")
            } else {
                createKline(String.format("%02d", i), "90.2", "90.2", "90.2", "90.2")
            }
        }
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
