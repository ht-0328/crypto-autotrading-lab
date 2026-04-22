package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.ApiConfig
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GmoPublicApiClientTest {

    private val apiConfig = ApiConfig(
        baseUrl = "https://api.coin.z.com/public",
        retryCount = 1
    )

    private fun createMockClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun `getTickerが成功時に正しくレスポンスをパースできること`() = runTest {
        // Arrange
        val jsonResponse = """
            {
                "status": 0,
                "data": [
                    {
                        "ask": "1000000",
                        "bid": "990000",
                        "high": "1050000",
                        "last": "995000",
                        "low": "980000",
                        "symbol": "BTC",
                        "timestamp": "2023-01-01T00:00:00.000Z",
                        "volume": "100.0"
                    }
                ],
                "responsetime": "2023-01-01T00:00:00.000Z"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = jsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val apiClient = GmoPublicApiClient(apiConfig.baseUrl ?: "https://api.coin.z.com/public", httpClient)

        // Act
        val response = apiClient.getTicker("BTC")

        // Assert
        assertEquals(0, response.status)
        assertEquals(1, response.data.size)
        val ticker = response.data[0]
        assertEquals("BTC", ticker.symbol)
        assertEquals("995000", ticker.last)
    }

    @Test
    fun `getKlinesが成功時に正しくレスポンスをパースできること`() = runTest {
        // Arrange
        val jsonResponse = """
            {
                "status": 0,
                "data": [
                    {
                        "openTime": "1672531200000",
                        "open": "1000000",
                        "high": "1050000",
                        "low": "980000",
                        "close": "995000",
                        "volume": "100.0"
                    }
                ],
                "responsetime": "2023-01-01T00:00:00.000Z"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = jsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val apiClient = GmoPublicApiClient(apiConfig.baseUrl ?: "https://api.coin.z.com/public", httpClient)

        // Act
        val response = apiClient.getKlines("BTC", "5min", "20230101")

        // Assert
        assertEquals(0, response.status)
        assertEquals(1, response.data.size)
        val kline = response.data[0]
        assertEquals("1672531200000", kline.openTime)
        assertEquals("995000", kline.close)
    }

    @Test
    fun `getTickerが200以外のレスポンス時に例外をスローすること`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val apiClient = GmoPublicApiClient(apiConfig.baseUrl ?: "https://api.coin.z.com/public", httpClient)

        // Act & Assert
        assertThrows<Exception> {
            apiClient.getTicker("BTC")
        }
    }
}
