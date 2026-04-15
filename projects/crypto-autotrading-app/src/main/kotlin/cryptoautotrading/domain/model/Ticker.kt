package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Ticker(
    val ask: String,
    val bid: String,
    val high: String,
    val last: String,
    val low: String,
    val symbol: String,
    val timestamp: String,
    val volume: String
)

@Serializable
data class TickerResponse(
    val status: Int,
    val data: List<Ticker>,
    val responsetime: String
)
