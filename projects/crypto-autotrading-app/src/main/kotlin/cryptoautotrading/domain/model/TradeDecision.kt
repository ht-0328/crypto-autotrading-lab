package cryptoautotrading.domain.model

import java.math.BigDecimal

data class TradeDecision(
    val action: TradeAction,
    val reason: String,
    val atr: BigDecimal? = null
)
