package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.strategy.TradingStrategy
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BacktestEngineTest {

    @Test
    fun `初期資金10000で取引を行わない場合、終了時の総資産は10000のままであること`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        // 常に「見送り」を返す戦略
        every { mockStrategy.judge(any(), any()) } returns TradeDecision(TradeAction.SKIP, "テスト用見送り")

        val klines = listOf(
            Kline("202605010000", "100", "110", "90", "105", "10"),
            Kline("202605010005", "105", "115", "95", "110", "15")
        )
        val initialCapital = BigDecimal("10000")
        val tradeAmount = 1000

        // Act
        val result = engine.run(klines, mockStrategy, initialCapital, tradeAmount)

        // Assert
        val summary = result.summary
        assertEquals(0, BigDecimal("10000").compareTo(summary.initialCapital))
        assertEquals(0, BigDecimal("10000").compareTo(summary.finalAssetValue))
        assertEquals(0, summary.buyCount)
        assertEquals(0, summary.sellCount)
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.maxDrawdown))

        assertEquals(2, result.steps.size)
        assertEquals(TradeAction.SKIP, result.steps[0].action)
        assertEquals(0, BigDecimal("10000").compareTo(result.steps[0].cashBalance))
    }
}
