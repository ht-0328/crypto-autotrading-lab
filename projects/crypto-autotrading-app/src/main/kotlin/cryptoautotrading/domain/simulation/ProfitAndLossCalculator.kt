package cryptoautotrading.domain.simulation

import java.math.BigDecimal

data class ProfitAndLossResult(
    val profitAndLoss: BigDecimal = BigDecimal.ZERO,
    val estimatedProfitAndLoss: BigDecimal = BigDecimal.ZERO
)

class ProfitAndLossCalculator {
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
