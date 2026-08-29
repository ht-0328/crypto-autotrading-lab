package cryptoautotrading.domain.realtrading

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderSizeSpecTest {

    private val spec = OrderSizeSpec(
        minOrderSize = BigDecimal("0.00001"),
        sizeStep = BigDecimal("0.00001")
    )

    @Test
    fun `刻みに合わない数量は切り捨てられること`() {
        // 1000円 ÷ 12,447,381円 = 0.00008033... を刻みに丸める
        val rawSize = BigDecimal("0.00008033")

        val rounded = spec.roundDownToStep(rawSize)

        assertEquals(0, BigDecimal("0.00008").compareTo(rounded))
    }

    @Test
    fun `丸めた数量が刻みの整数倍として文字列化されること`() {
        // 取引所へは文字列で送るため、指数表記にならないことを確認する
        val rounded = spec.roundDownToStep(BigDecimal("0.00008033"))

        assertEquals("0.00008", rounded.toPlainString())
    }

    @Test
    fun `ちょうど刻みの整数倍の数量はそのままであること`() {
        val rounded = spec.roundDownToStep(BigDecimal("0.00012"))

        assertEquals(0, BigDecimal("0.00012").compareTo(rounded))
    }

    @Test
    fun `刻みに満たない数量は0に切り捨てられること`() {
        val rounded = spec.roundDownToStep(BigDecimal("0.000009"))

        assertEquals(0, BigDecimal.ZERO.compareTo(rounded))
    }

    @Test
    fun `最小注文数量以上なら発注できると判定されること`() {
        assertTrue(spec.isTradable(BigDecimal("0.00001")))
        assertTrue(spec.isTradable(BigDecimal("0.00008")))
    }

    @Test
    fun `最小注文数量に満たない数量は発注できないと判定されること`() {
        assertFalse(spec.isTradable(BigDecimal("0.000009")))
        assertFalse(spec.isTradable(BigDecimal.ZERO))
    }

    @Test
    fun `最小注文数量に満たない端数は保有とみなされないこと`() {
        // 手数料や丸めで残るダストを保有とみなすと、以後1回も買えなくなる
        assertFalse(spec.isHoldingAmount(BigDecimal("0.000005")))
    }

    @Test
    fun `最小注文数量以上の残高は保有とみなされること`() {
        assertTrue(spec.isHoldingAmount(BigDecimal("0.00001")))
    }

    @Test
    fun `最小注文数量が0以下の場合は生成できないこと`() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderSizeSpec(minOrderSize = BigDecimal.ZERO, sizeStep = BigDecimal("0.00001"))
        }
    }

    @Test
    fun `刻みが0以下の場合は生成できないこと`() {
        assertThrows(IllegalArgumentException::class.java) {
            OrderSizeSpec(minOrderSize = BigDecimal("0.00001"), sizeStep = BigDecimal.ZERO)
        }
    }
}
