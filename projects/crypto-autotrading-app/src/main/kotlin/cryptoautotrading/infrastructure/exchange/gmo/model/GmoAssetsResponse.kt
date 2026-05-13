package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoAssetsResponse(val status: Int, val data: List<GmoAssetData>?, val messages: List<GmoMessage>? = null)
