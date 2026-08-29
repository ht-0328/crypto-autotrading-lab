package cryptoautotrading.domain.realtrading

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 取引所が定める注文数量の制約を表す値オブジェクト。
 *
 * 取引所は最小注文数量と数量の刻みを持つ。刻みに合わない数量を送ると注文は拒否される。
 * 例えば GMOコインの BTC は最小注文数量・刻みとも 0.00001 で、0.00008033 のような
 * 数量は受け付けられない。
 *
 * @property minOrderSize 最小注文数量。これを下回る注文は送れない
 * @property sizeStep 注文数量の刻み。注文数量はこの値の整数倍でなければならない
 */
data class OrderSizeSpec(
    val minOrderSize: BigDecimal,
    val sizeStep: BigDecimal
) {

    init {
        require(minOrderSize > BigDecimal.ZERO) { "minOrderSize は正の数である必要があります: $minOrderSize" }
        require(sizeStep > BigDecimal.ZERO) { "sizeStep は正の数である必要があります: $sizeStep" }
    }

    /**
     * 注文数量を刻みに合わせて切り捨てる。
     *
     * 切り上げると、想定していた注文金額と上限を超える可能性があるため切り捨てる。
     *
     * @param size 丸める前の数量
     * @return 刻みの整数倍に切り捨てた数量
     */
    fun roundDownToStep(size: BigDecimal): BigDecimal {
        val steps = size.divide(sizeStep, 0, RoundingMode.DOWN)
        return steps.multiply(sizeStep)
    }

    /**
     * 取引所に送れる数量かどうかを判定する。
     *
     * @param size 判定する数量
     * @return 最小注文数量以上であれば true
     */
    fun isTradable(size: BigDecimal): Boolean {
        return size >= minOrderSize
    }

    /**
     * 保有しているとみなせる数量かどうかを判定する。
     *
     * 手数料や丸めによって最小注文数量に満たない端数（ダスト）が残ることがある。
     * ダストを保有とみなすと、二重保有防止のチェックに永久に引っかかり、
     * 以後1回も注文できなくなるため、保有とはみなさない。
     *
     * @param amount 取引所から取得した保有数量
     * @return 最小注文数量以上であれば true
     */
    fun isHoldingAmount(amount: BigDecimal): Boolean {
        return amount >= minOrderSize
    }
}
