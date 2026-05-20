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
        publicBaseUrl = "https://api.coin.z.com",
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
        val apiClient = GmoPublicApiClient(apiConfig.publicBaseUrl ?: "https://api.coin.z.com", apiConfig.retryCount, httpClient)

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
        val apiClient = GmoPublicApiClient(apiConfig.publicBaseUrl ?: "https://api.coin.z.com", apiConfig.retryCount, httpClient)

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
        val apiClient = GmoPublicApiClient(apiConfig.publicBaseUrl ?: "https://api.coin.z.com", apiConfig.retryCount, httpClient)

        // Act & Assert
        assertThrows<Exception> {
            apiClient.getTicker("BTC")
        }
    }

    @Test
    fun `getTickerが一時的に失敗しても指定回数内で成功すれば結果を返すこと`() = runTest {
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

        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond(
                    content = "Internal Server Error",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            } else {
                respond(
                    content = jsonResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        // retryCount = 1
        val apiClient = GmoPublicApiClient(apiConfig.publicBaseUrl ?: "https://api.coin.z.com", apiConfig.retryCount, httpClient)

        // Act
        val response = apiClient.getTicker("BTC")

        // Assert
        assertEquals(2, callCount)
        assertEquals(0, response.status)
        assertEquals(1, response.data.size)
        assertEquals("BTC", response.data[0].symbol)
    }

    @Test
    fun `getTickerが指定されたリトライ回数を超えて失敗した場合は例外をスローすること`() = runTest {
        // Arrange
        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
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
        // retryCount = 1
        val apiClient = GmoPublicApiClient(apiConfig.publicBaseUrl ?: "https://api.coin.z.com", apiConfig.retryCount, httpClient)

        // Act & Assert
        assertThrows<Exception> {
            apiClient.getTicker("BTC")
        }
        // 最初の呼び出し1回 + リトライ1回 = 2回
        assertEquals(2, callCount)
    }
}
