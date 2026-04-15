package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.KlineResponse
import cryptoautotrading.domain.model.TickerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GmoPublicApiClient(private val baseUrl: String) : AutoCloseable {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getTicker(symbol: String): TickerResponse {
        return client.get("$baseUrl/public/v1/ticker") {
            parameter("symbol", symbol)
        }.body()
    }

    suspend fun getKlines(symbol: String, interval: String, date: String): KlineResponse {
        return client.get("$baseUrl/public/v1/klines") {
            parameter("symbol", symbol)
            parameter("interval", interval)
            parameter("date", date)
        }.body()
    }

    override fun close() {
        client.close()
    }
}
