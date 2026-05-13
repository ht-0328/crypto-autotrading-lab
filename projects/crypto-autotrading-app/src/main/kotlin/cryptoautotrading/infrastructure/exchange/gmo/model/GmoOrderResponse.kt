package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoOrderResponse(val status: Int, val data: String?, val messages: List<GmoMessage>? = null)
