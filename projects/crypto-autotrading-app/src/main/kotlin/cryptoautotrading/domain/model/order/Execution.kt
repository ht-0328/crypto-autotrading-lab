package cryptoautotrading.domain.model.order
import java.math.BigDecimal
data class Execution(val executionId: String, val orderId: String, val price: BigDecimal, val size: BigDecimal)
