package cryptoautotrading.domain.realtrading

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 注文価格と手数料に関する制約を表す値オブジェクト。
 *
 * 注文数量は「注文金額 ÷ 価格」で決まるため、古い価格を使うと想定より多い数量になり、
 * 実際の約定金額が上限を超える。また成行注文は板の状況によって想定と違う価格で約定する。
 * ここではその2つを扱う。
 *
 * @property takerFeeRate 成行注文の手数料率
 * @property maxSlippageRate 許容するスリッページの割合
 */
data class OrderPriceSpec(
    val takerFeeRate: BigDecimal,
    val maxSlippageRate: BigDecimal
) {

    init {
        require(takerFeeRate >= BigDecimal.ZERO) { "takerFeeRate は0以上である必要があります: $takerFeeRate" }
        require(maxSlippageRate > BigDecimal.ZERO) { "maxSlippageRate は正の数である必要があります: $maxSlippageRate" }
    }

    /**
     * 2つの価格の乖離が許容範囲に収まっているかを判定する。
     *
     * @param basePrice 基準になる価格
     * @param comparedPrice 比較する価格
     * @return 乖離が許容範囲内であれば true
     */
    fun isWithinAllowedSlippage(basePrice: BigDecimal, comparedPrice: BigDecimal): Boolean {
        if (basePrice <= BigDecimal.ZERO) {
            return false
        }
        return calculateDivergenceRate(basePrice, comparedPrice) <= maxSlippageRate
    }

    /**
     * 2つの価格の乖離の割合を計算する。
     *
     * @param basePrice 基準になる価格
     * @param comparedPrice 比較する価格
     * @return 乖離の割合（絶対値）
     */
    fun calculateDivergenceRate(basePrice: BigDecimal, comparedPrice: BigDecimal): BigDecimal {
        return comparedPrice.subtract(basePrice)
            .abs()
            .divide(basePrice, DIVERGENCE_SCALE, RoundingMode.HALF_UP)
    }

    /**
     * 手数料を含めた注文金額を計算する。
     *
     * 上限判定に使うため、端数は切り上げて安全側に倒す。
     *
     * @param orderAmountJpy 手数料を含まない注文金額(JPY)
     * @return 手数料を含めた注文金額(JPY)
     */
    fun calculateTotalCostWithFee(orderAmountJpy: Int): Int {
        val amount = BigDecimal(orderAmountJpy)
        val fee = amount.multiply(takerFeeRate)
        return amount.add(fee).setScale(0, RoundingMode.UP).toInt()
    }

    /**
     * 手数料を含めても指定の残高に収まる注文金額を計算する。
     *
     * 残高の全額を注文金額にすると、手数料の分だけ残高が足りなくなる。
     *
     * @param availableJpy 注文に使える残高(JPY)
     * @return 手数料を含めて残高に収まる注文金額(JPY)
     */
    fun calculateAffordableOrderAmount(availableJpy: BigDecimal): Int {
        return availableJpy
            .divide(BigDecimal.ONE.add(takerFeeRate), 0, RoundingMode.DOWN)
            .toInt()
    }

    private companion object {
        /** 乖離の割合を計算するときの小数点以下の桁数 */
        const val DIVERGENCE_SCALE = 8
    }
}
