package cryptoautotrading.domain.simulation

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfitAndLossCalculatorTest {

    private val calculator = ProfitAndLossCalculator()

    @Test
    fun `calculate - 未保有の場合は損益ゼロを返すこと`() {
        // Arrange & Act
        val result = calculator.calculate(
            isHolding = false,
            currentPrice = BigDecimal("1500.0"),
            buyPrice = BigDecimal("1000.0"),
            holdingAmount = BigDecimal("2.0"),
            shouldSell = true
        )

        // Assert
        assertEquals(BigDecimal.ZERO, result.profitAndLoss)
        assertEquals(BigDecimal.ZERO, result.estimatedProfitAndLoss)
    }

    @Test
    fun `calculate - 保有中で売却しない場合は想定損益のみ計算されること`() {
        // Arrange
        val currentPrice = BigDecimal("1500.0")
        val buyPrice = BigDecimal("1000.0")
        val holdingAmount = BigDecimal("2.0") // 利益: (1500 - 1000) * 2 = 1000

        // Act
        val result = calculator.calculate(
            isHolding = true,
            currentPrice = currentPrice,
            buyPrice = buyPrice,
            holdingAmount = holdingAmount,
            shouldSell = false
        )

        // Assert
        assertEquals(BigDecimal.ZERO, result.profitAndLoss)
        assertEquals(BigDecimal("1000.00"), result.estimatedProfitAndLoss)
    }

    @Test
    fun `calculate - 保有中で売却する場合は確定損益と想定損益が計算されること`() {
        // Arrange
        val currentPrice = BigDecimal("1500.0")
        val buyPrice = BigDecimal("1000.0")
        val holdingAmount = BigDecimal("2.0") // 利益: (1500 - 1000) * 2 = 1000

        // Act
        val result = calculator.calculate(
            isHolding = true,
            currentPrice = currentPrice,
            buyPrice = buyPrice,
            holdingAmount = holdingAmount,
            shouldSell = true
        )

        // Assert
        assertEquals(BigDecimal("1000.00"), result.profitAndLoss)
        assertEquals(BigDecimal("1000.00"), result.estimatedProfitAndLoss)
    }

    @Test
    fun `calculate - 保有中で損失が出ている場合の計算`() {
        // Arrange
        val currentPrice = BigDecimal("800.0")
        val buyPrice = BigDecimal("1000.0")
        val holdingAmount = BigDecimal("3.0") // 損失: (800 - 1000) * 3 = -600

        // Act
        val result = calculator.calculate(
            isHolding = true,
            currentPrice = currentPrice,
            buyPrice = buyPrice,
            holdingAmount = holdingAmount,
            shouldSell = true
        )

        // Assert
        assertEquals(BigDecimal("-600.00"), result.profitAndLoss)
        assertEquals(BigDecimal("-600.00"), result.estimatedProfitAndLoss)
    }
}
