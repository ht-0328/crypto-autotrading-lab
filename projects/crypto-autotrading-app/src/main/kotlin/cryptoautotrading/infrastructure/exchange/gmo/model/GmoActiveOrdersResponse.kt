package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoActiveOrdersResponse(val status: Int, val data: GmoActiveOrdersData?, val messages: List<GmoMessage>? = null)
