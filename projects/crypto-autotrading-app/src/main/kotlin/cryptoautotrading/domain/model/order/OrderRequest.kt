package cryptoautotrading.domain.model.order
import java.math.BigDecimal
data class OrderRequest(
    val symbol: String,
    val side: String,
    val executionType: String,
    val timeInForce: String,
    val price: BigDecimal?,
    val size: BigDecimal
)
