package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Kline(
    val openTime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String
)

@Serializable
data class KlineResponse(
    val status: Int,
    val data: List<Kline>,
    val responsetime: String
)
