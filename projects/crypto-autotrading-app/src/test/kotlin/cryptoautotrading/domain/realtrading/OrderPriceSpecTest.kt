package cryptoautotrading.domain.realtrading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderPriceSpecTest {

    private val spec = OrderPriceSpec(
        takerFeeRate = BigDecimal("0.0005"),
        maxSlippageRate = BigDecimal("0.005")
    )

    @Test
    fun `乖離が許容範囲内なら許容と判定されること`() {
        // 1,000,000 に対して 0.4% 上振れ
        assertTrue(spec.isWithinAllowedSlippage(BigDecimal("1000000"), BigDecimal("1004000")))
    }

    @Test
    fun `乖離がちょうど許容範囲なら許容と判定されること`() {
        assertTrue(spec.isWithinAllowedSlippage(BigDecimal("1000000"), BigDecimal("1005000")))
    }

    @Test
    fun `乖離が許容範囲を超えたら許容しないと判定されること`() {
        assertFalse(spec.isWithinAllowedSlippage(BigDecimal("1000000"), BigDecimal("1006000")))
    }

    @Test
    fun `下振れの乖離も許容範囲を超えたら許容しないと判定されること`() {
        // 上下どちらにずれても不利になりうるため絶対値で判定する
        assertFalse(spec.isWithinAllowedSlippage(BigDecimal("1000000"), BigDecimal("994000")))
    }

    @Test
    fun `基準価格が0以下なら許容しないと判定されること`() {
        assertFalse(spec.isWithinAllowedSlippage(BigDecimal.ZERO, BigDecimal("1000000")))
    }

    @Test
    fun `手数料を含めた注文金額が切り上げで計算されること`() {
        // 10000円 × 0.05% = 5円
        assertEquals(10005, spec.calculateTotalCostWithFee(10000))
    }

    @Test
    fun `手数料の端数は切り上げられること`() {
        // 1000円 × 0.05% = 0.5円 なので、安全側に倒して 1001円 とする
        assertEquals(1001, spec.calculateTotalCostWithFee(1000))
    }

    @Test
    fun `残高から手数料を差し引いた注文金額が計算されること`() {
        // 15500.5 ÷ 1.0005 = 15492.75... を切り捨てる
        assertEquals(15492, spec.calculateAffordableOrderAmount(BigDecimal("15500.5")))
    }

    @Test
    fun `残高から計算した注文金額は手数料を含めても残高に収まること`() {
        val available = BigDecimal("15500.5")

        val orderAmount = spec.calculateAffordableOrderAmount(available)
        val totalCost = spec.calculateTotalCostWithFee(orderAmount)

        assertTrue(BigDecimal(totalCost) <= available)
    }

    @Test
    fun `許容スリッページが0以下の場合は生成できないこと`() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderPriceSpec(takerFeeRate = BigDecimal("0.0005"), maxSlippageRate = BigDecimal.ZERO)
        }
    }

    @Test
    fun `手数料率が負の場合は生成できないこと`() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderPriceSpec(takerFeeRate = BigDecimal("-0.0001"), maxSlippageRate = BigDecimal("0.005"))
        }
    }
}
