package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoOrderRequest(val symbol: String, val side: String, val executionType: String, val timeInForce: String, val price: String?, val size: String)
