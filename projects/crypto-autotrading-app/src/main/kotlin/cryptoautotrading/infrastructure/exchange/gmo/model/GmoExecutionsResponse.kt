package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoExecutionsResponse(val status: Int, val data: GmoExecutionsData?, val messages: List<GmoMessage>? = null)
