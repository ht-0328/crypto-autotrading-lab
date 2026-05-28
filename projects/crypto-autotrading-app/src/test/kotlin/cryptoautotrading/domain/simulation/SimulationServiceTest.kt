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
        val currentPrice = BigDecimal("30000.0") // Indivisible price
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(currentPrice, nextState.buyPrice)
        val expectedAmount = BigDecimal(tradeAmount).divide(currentPrice, 8, RoundingMode.DOWN)
        val actualBuyAmount = expectedAmount * currentPrice
        assertEquals(expectedAmount, nextState.holdingAmount)
        assertEquals(0, BigDecimal("20000").subtract(actualBuyAmount).compareTo(nextState.cashBalance))
        assertNotEquals(currentState.lastUpdatedAt, nextState.lastUpdatedAt)
    }

    @Test
    fun `BUY時の資金計算テスト_実際の購入金額のみが引かれること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("10000"),
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("30000.0") // Indivisible price
        val tradeAmount = 1000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        val expectedAmount = BigDecimal(tradeAmount).divide(currentPrice, 8, RoundingMode.DOWN)
        val actualBuyAmount = expectedAmount * currentPrice
        assertEquals(0, BigDecimal("10000").subtract(actualBuyAmount).compareTo(nextState.cashBalance))

        // Ensure cashBalance + actualBuyAmount equals initial cashBalance
        val estimatedHoldingValue = expectedAmount * currentPrice
        assertEquals(0, BigDecimal("10000").compareTo(nextState.cashBalance + estimatedHoldingValue))
    }

    @Test
    fun `BUYからSELL後の整合性テスト_総資産と初期資金プラス確定損益が一致すること`() {
        // Arrange
        val initialCapital = BigDecimal("10000")
        val currentState = SimulationState(
            cashBalance = initialCapital,
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val buyDecision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val buyPrice = BigDecimal("30000.0") // Indivisible price
        val tradeAmount = 1000

        // Act (Buy)
        val stateAfterBuy = simulationService.updateState(currentState, buyDecision, buyPrice, tradeAmount)

        // Act (Sell)
        val sellDecision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val sellPrice = BigDecimal("33000.0")
        val finalState = simulationService.updateState(stateAfterBuy, sellDecision, sellPrice, tradeAmount)

        // Assert
        assertFalse(finalState.isHolding)
        val expectedAmount = BigDecimal(tradeAmount).divide(buyPrice, 8, RoundingMode.DOWN)
        val expectedBuyAmount = expectedAmount * buyPrice
        val expectedSellAmount = expectedAmount * sellPrice
        val expectedProfit = expectedSellAmount - expectedBuyAmount

        assertEquals(0, expectedProfit.compareTo(finalState.realizedProfitAndLoss))
        assertEquals(0, (initialCapital + finalState.realizedProfitAndLoss).compareTo(finalState.cashBalance))
    }

    @Test
    fun `複数回売買時の累積誤差テスト_総資産と確定損益が乖離しないこと`() {
        // Arrange
        val initialCapital = BigDecimal("10000")
        var state = SimulationState(
            cashBalance = initialCapital,
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val tradeAmount = 1000
        val prices = listOf(
            BigDecimal("30000.0"), BigDecimal("31000.0"), // Buy, Sell
            BigDecimal("31000.0"), BigDecimal("29000.0"), // Buy, Sell (Loss)
            BigDecimal("25000.0"), BigDecimal("26000.0")  // Buy, Sell
        )

        // Act
        for (i in prices.indices step 2) {
            val buyPrice = prices[i]
            val sellPrice = prices[i+1]

            // Buy
            val buyDecision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
            state = simulationService.updateState(state, buyDecision, buyPrice, tradeAmount)

            // Sell
            val sellDecision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
            state = simulationService.updateState(state, sellDecision, sellPrice, tradeAmount)
        }

        // Assert
        assertFalse(state.isHolding)
        assertEquals(0, (initialCapital + state.realizedProfitAndLoss).compareTo(state.cashBalance))
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
        assertEquals("", nextState.lastStopLossTime) // Profit >= 0, so no stop loss time update
    }

    @Test
    fun `判定がSELL_CANDIDATEで損切りの場合、lastStopLossTimeが更新されること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("10000"),
            isHolding = true,
            buyPrice = BigDecimal("50000.0"),
            holdingAmount = BigDecimal("0.25"),
            realizedProfitAndLoss = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00",
            lastStopLossTime = ""
        )
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val currentPrice = BigDecimal("40000.0") // Price dropped
        val tradeAmount = 10000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount, "2023-01-01T01:00:00")

        // Assert
        assertFalse(nextState.isHolding)
        // sellAmount = 0.25 * 40000 = 10000
        // buyAmount = 0.25 * 50000 = 12500
        // profitAndLoss = -2500
        assertEquals(BigDecimal("-2500.00"), nextState.realizedProfitAndLoss.setScale(2))
        assertEquals("2023-01-01T01:00:00", nextState.lastStopLossTime)
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
    fun `SimulationState更新時にrealTradingが引き継がれること`() {
        // Arrange
        val initialRealTradingState = cryptoautotrading.domain.model.realtrading.RealTradingState(
            isStopped = true,
            stopReason = "Test Error"
        )
        val currentState = SimulationState(
            cashBalance = BigDecimal("10000"),
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00",
            realTrading = initialRealTradingState
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("50000.0")
        val tradeAmount = 1000

        // Act
        val nextState = simulationService.updateState(currentState, decision, currentPrice, tradeAmount)

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(initialRealTradingState, nextState.realTrading)
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

    @Test
    fun `判定がBUY_CANDIDATEかつALL_INの場合、cashBalanceを全て使って購入状態に更新されること`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("15000"),
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("30000.0")
        val tradeAmount = 5000 // This should be ignored

        // Act
        val nextState = simulationService.updateState(
            currentState = currentState,
            decision = decision,
            currentPrice = currentPrice,
            tradeAmount = tradeAmount,
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        // Assert
        assertTrue(nextState.isHolding)
        assertEquals(currentPrice, nextState.buyPrice)
        val expectedAmount = BigDecimal("15000").divide(currentPrice, 8, RoundingMode.DOWN)
        val actualBuyAmount = expectedAmount * currentPrice
        assertEquals(expectedAmount, nextState.holdingAmount)
        assertEquals(0, BigDecimal("15000").subtract(actualBuyAmount).compareTo(nextState.cashBalance))
    }

    @Test
    fun `ALL_INですでに保有中の場合は追加で購入されないこと`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal("15000"),
            isHolding = true,
            buyPrice = BigDecimal("20000"),
            holdingAmount = BigDecimal("0.5"),
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("30000.0")

        // Act
        val nextState = simulationService.updateState(
            currentState = currentState,
            decision = decision,
            currentPrice = currentPrice,
            tradeAmount = 5000,
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        // Assert
        assertEquals(currentState.cashBalance, nextState.cashBalance)
        assertTrue(nextState.isHolding)
        assertEquals(currentState.holdingAmount, nextState.holdingAmount)
    }

    @Test
    fun `ALL_INでcashBalanceが0以下の場合は購入されないこと`() {
        // Arrange
        val currentState = SimulationState(
            cashBalance = BigDecimal.ZERO,
            isHolding = false,
            buyPrice = BigDecimal.ZERO,
            holdingAmount = BigDecimal.ZERO,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val currentPrice = BigDecimal("30000.0")

        // Act
        val nextState = simulationService.updateState(
            currentState = currentState,
            decision = decision,
            currentPrice = currentPrice,
            tradeAmount = 10000,
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        // Assert
        assertFalse(nextState.isHolding)
        assertEquals(0, BigDecimal.ZERO.compareTo(nextState.cashBalance))
    }
}
