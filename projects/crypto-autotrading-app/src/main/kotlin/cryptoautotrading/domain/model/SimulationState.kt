package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SimulationState(
    @Serializable(with = BigDecimalSerializer::class)
    val cashBalance: BigDecimal = BigDecimal.ZERO,

    val isHolding: Boolean = false,

    @Serializable(with = BigDecimalSerializer::class)
    val buyPrice: BigDecimal = BigDecimal.ZERO,

    @Serializable(with = BigDecimalSerializer::class)
    val holdingAmount: BigDecimal = BigDecimal.ZERO,

    @Serializable(with = BigDecimalSerializer::class)
    val realizedProfitAndLoss: BigDecimal = BigDecimal.ZERO,

    val lastUpdatedAt: String = "",

    val lastStopLossTime: String = "",

    @Serializable(with = BigDecimalSerializer::class)
    val entryAtr: BigDecimal? = null
)
