package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.TradeAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BacktestResultTest {

    @Test
    fun `BacktestSummaryのインスタンスが正しく生成されること`() {
        val summary = BacktestSummary(
            strategyName = "TestStrategy",
            initialCapital = BigDecimal("1000000"),
            finalAssetValue = BigDecimal("1100000"),
            realizedProfitAndLoss = BigDecimal("100000"),
            totalReturnRate = BigDecimal("0.10"),
            tradeCount = 10,
            buyCount = 5,
            sellCount = 5,
            maxDrawdown = BigDecimal("0.05")
        )

        assertEquals("TestStrategy", summary.strategyName)
        assertEquals(BigDecimal("1000000"), summary.initialCapital)
        assertEquals(BigDecimal("1100000"), summary.finalAssetValue)
        assertEquals(BigDecimal("100000"), summary.realizedProfitAndLoss)
        assertEquals(BigDecimal("0.10"), summary.totalReturnRate)
        assertEquals(10, summary.tradeCount)
        assertEquals(5, summary.buyCount)
        assertEquals(5, summary.sellCount)
        assertEquals(BigDecimal("0.05"), summary.maxDrawdown)
    }

    @Test
    fun `BacktestStepResultのインスタンスが正しく生成されること`() {
        val step = BacktestStepResult(
            openTime = "2026-05-01T00:00:00Z",
            close = BigDecimal("5000000"),
            action = TradeAction.BUY_CANDIDATE,
            reason = "Test Reason",
            cashBalance = BigDecimal("500000"),
            holdingAmount = BigDecimal("0.1"),
            buyPrice = BigDecimal("5000000"),
            realizedProfitAndLoss = BigDecimal("0"),
            estimatedHoldingValue = BigDecimal("500000"),
            totalAssetValue = BigDecimal("1000000")
        )

        assertEquals("2026-05-01T00:00:00Z", step.openTime)
        assertEquals(BigDecimal("5000000"), step.close)
        assertEquals(TradeAction.BUY_CANDIDATE, step.action)
        assertEquals("Test Reason", step.reason)
        assertEquals(BigDecimal("500000"), step.cashBalance)
        assertEquals(BigDecimal("0.1"), step.holdingAmount)
        assertEquals(BigDecimal("5000000"), step.buyPrice)
        assertEquals(BigDecimal("0"), step.realizedProfitAndLoss)
        assertEquals(BigDecimal("500000"), step.estimatedHoldingValue)
        assertEquals(BigDecimal("1000000"), step.totalAssetValue)
    }

    @Test
    fun `BacktestResultのインスタンスが正しく生成されること`() {
        val summary = BacktestSummary(
            strategyName = "TestStrategy",
            initialCapital = BigDecimal("1000000"),
            finalAssetValue = BigDecimal("1100000"),
            realizedProfitAndLoss = BigDecimal("100000"),
            totalReturnRate = BigDecimal("0.10"),
            tradeCount = 10,
            buyCount = 5,
            sellCount = 5,
            maxDrawdown = BigDecimal("0.05")
        )

        val step = BacktestStepResult(
            openTime = "2026-05-01T00:00:00Z",
            close = BigDecimal("5000000"),
            action = TradeAction.BUY_CANDIDATE,
            reason = "Test Reason",
            cashBalance = BigDecimal("500000"),
            holdingAmount = BigDecimal("0.1"),
            buyPrice = BigDecimal("5000000"),
            realizedProfitAndLoss = BigDecimal("0"),
            estimatedHoldingValue = BigDecimal("500000"),
            totalAssetValue = BigDecimal("1000000")
        )

        val result = BacktestResult(
            summary = summary,
            steps = listOf(step)
        )

        assertEquals(summary, result.summary)
        assertEquals(1, result.steps.size)
        assertEquals(step, result.steps[0])
    }
}
