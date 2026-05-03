package cryptoautotrading.domain.simulation

import java.math.BigDecimal

data class ProfitAndLossResult(
    val profitAndLoss: BigDecimal = BigDecimal.ZERO,
    val estimatedProfitAndLoss: BigDecimal = BigDecimal.ZERO
)

/**
 * 取引による損益を計算するクラス。
 */
class ProfitAndLossCalculator {
    /**
     * 現在の保有状況や価格に基づいて、確定損益および想定損益（含み損益）を計算する。
     *
     * @param isHolding ポジションを保有しているか
     * @param currentPrice 現在の価格
     * @param buyPrice 購入したときの価格
     * @param holdingAmount 保有している数量
     * @param shouldSell 売却すべき状態か（確定損益として計上するか）
     * @return 確定損益および想定損益を含んだ計算結果
     */
    fun calculate(
        isHolding: Boolean,
        currentPrice: BigDecimal,
        buyPrice: BigDecimal,
        holdingAmount: BigDecimal,
        shouldSell: Boolean
    ): ProfitAndLossResult {
        if (!isHolding) {
            return ProfitAndLossResult()
        }

        val estimated = (currentPrice - buyPrice) * holdingAmount
        val actual = if (shouldSell) estimated else BigDecimal.ZERO
        return ProfitAndLossResult(
            profitAndLoss = actual,
            estimatedProfitAndLoss = estimated
        )
    }
}
