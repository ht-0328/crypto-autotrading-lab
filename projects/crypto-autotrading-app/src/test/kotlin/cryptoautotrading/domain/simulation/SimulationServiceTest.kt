package cryptoautotrading.domain.simulation

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class SimulationServiceTest {

    private val simulationService = SimulationService()

    @Test
    fun `判定がBUY_CANDIDATEかつ未保有の場合、購入状態に更新されること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("20000"),
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(currentPrice, nextState.buyPrice)
        val expectedAmount = BigDecimal(tradeAmount).divide(currentPrice, 8, RoundingMode.DOWN)
        assertEquals(expectedAmount, nextState.holdingAmount)
        assertEquals(BigDecimal("10000"), nextState.cashBalance) // 20000 - 10000
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がBUY_CANDIDATEでも、残金が足りない場合は状態が維持されること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("5000"),
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(BigDecimal("5000"), nextState.cashBalance)
        assertEquals(BigDecimal.ZERO, nextState.buyPrice)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がBUY_CANDIDATEかつ既に保有中の場合、状態が維持されること`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("40000.0"),
            holdingAmount = BigDecimal("0.25"),
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(currentState.buyPrice, nextState.buyPrice)
        assertEquals(currentState.holdingAmount, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がSELL_CANDIDATEかつ保有中の場合、売却されて未保有状態に更新されること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("10000"),
            isHolding = true,
            buyPrice = BigDecimal("40000.0"),
            holdingAmount = BigDecimal("0.25"),
            realizedProfitAndLoss = BigDecimal("500"),
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(BigDecimal.ZERO, nextState.buyPrice)
        assertEquals(BigDecimal.ZERO, nextState.holdingAmount)

        // sellAmount = 0.25 * 50000 = 12500.00
        // buyAmount = 0.25 * 40000 = 10000.000
        // profitAndLoss = 12500.00 - 10000.000 = 2500.000
        // newCashBalance = 10000 + 12500.00 = 22500.00
        // newRealizedProfitAndLoss = 500 + 2500.000 = 3000.000
        assertEquals(BigDecimal("22500.00"), nextState.cashBalance.setScale(2))
        assertEquals(BigDecimal("3000.00"), nextState.realizedProfitAndLoss.setScale(2))

        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がSELL_CANDIDATEでbuyPriceが0以下の場合は売却されず状態が維持されること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("10000"),
            isHolding = true,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal("0.25"),
            realizedProfitAndLoss = BigDecimal("500"),
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(BigDecimal.ZERO, nextState.buyPrice)
        assertEquals(BigDecimal("0.25"), nextState.holdingAmount)
        assertEquals(BigDecimal("10000"), nextState.cashBalance)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がSELL_CANDIDATEかつ未保有の場合、状態が維持されること`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(BigDecimal.ZERO, nextState.buyPrice)
        assertEquals(BigDecimal.ZERO, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がSKIPの場合、状態が維持されること`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SKIP, "skip signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(currentState.buyPrice, nextState.buyPrice)
        assertEquals(currentState.holdingAmount, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `判定がHOLDINGの場合、状態が維持されること`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("40000.0"),
            holdingAmount = BigDecimal("0.25"),
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.HOLDING, "holding signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(currentState.buyPrice, nextState.buyPrice)
        assertEquals(currentState.holdingAmount, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }
}
