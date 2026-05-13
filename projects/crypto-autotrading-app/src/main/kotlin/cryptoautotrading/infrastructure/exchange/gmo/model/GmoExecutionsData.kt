package cryptoautotrading.infrastructure.exchange.gmo.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
@JsonIgnoreProperties(ignoreUnknown = true)
data class GmoExecutionsData(val list: List<GmoExecution>?)
