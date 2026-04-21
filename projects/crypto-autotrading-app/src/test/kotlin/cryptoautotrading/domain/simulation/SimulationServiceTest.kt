package cryptoautotrading.domain.simulation

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SimulationServiceTest {

    private val simulationService = SimulationService()

    @Test
    fun `updateState should buy when decision is BUY_CANDIDATE and not holding`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = false,
            buyPrice = 0.0,
            holdingAmount = 0.0,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = 50000.0
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(currentPrice, nextState.buyPrice)
        assertEquals(tradeAmount.toDouble() / currentPrice, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `updateState should maintain state when decision is BUY_CANDIDATE but already holding`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = true,
            buyPrice = 40000.0,
            holdingAmount = 0.25,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = 50000.0
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
    fun `updateState should sell when decision is SELL_CANDIDATE and holding`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = true,
            buyPrice = 40000.0,
            holdingAmount = 0.25,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val currentPrice = 50000.0
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(0.0, nextState.buyPrice)
        assertEquals(0.0, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `updateState should maintain state when decision is SELL_CANDIDATE but not holding`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = false,
            buyPrice = 0.0,
            holdingAmount = 0.0,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val currentPrice = 50000.0
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(0.0, nextState.buyPrice)
        assertEquals(0.0, nextState.holdingAmount)
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `updateState should maintain state when decision is SKIP`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = false,
            buyPrice = 0.0,
            holdingAmount = 0.0,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.SKIP, "skip signal")
        val currentPrice = 50000.0
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
    fun `updateState should maintain state when decision is HOLDING`() {
        // Arrange
        val currentState = SimulationState(
            isHolding = true,
            buyPrice = 40000.0,
            holdingAmount = 0.25,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.HOLDING, "holding signal")
        val currentPrice = 50000.0
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
