package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoAssetData(val symbol: String, val available: String)
