package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoCredential
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoCredentialProvider
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoSignatureGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class GmoPrivateApiClientImplTest {

    private val credentialProvider = object : GmoCredentialProvider {
        override fun getCredential() = GmoCredential("test_api_key", "test_secret_key")
    }

    private val signatureGenerator = object : GmoSignatureGenerator {
        override fun generate(
            timestamp: String, method: String, path: String, body: String, secretKey: String
        ): String = "dummy_signature"
    }

    private val clock = Clock.fixed(Instant.ofEpochMilli(1680000000000), ZoneId.of("UTC"))
    private val dtoMapper = GmoPrivateApiDtoMapper()
    private val baseUrl = "http://localhost:8080"

    @Test
    fun `getAssets が正しくマッピングされること`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("test_api_key", request.headers["API-KEY"])
            assertEquals("1680000000000", request.headers["API-TIMESTAMP"])
            assertEquals("dummy_signature", request.headers["API-SIGN"])

            respond(
                content = """
                    {
                      "status": 0,
                      "data": [
                        {
                          "amount": "1000",
                          "available": "1000",
                          "conversionRate": "1",
                          "symbol": "JPY"
                        }
                      ],
                      "responsetime": "2023-01-01T00:00:00.000Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GmoPrivateApiClientImpl(
            httpClient = HttpClient(mockEngine),
            baseUrl = baseUrl,
            signatureGenerator = signatureGenerator,
            credentialProvider = credentialProvider,
            dtoMapper = dtoMapper,
            clock = clock
        )

        val assets = client.getAssets()
        assertEquals(1, assets.size)
        assertEquals("JPY", assets[0].symbol)
        assertEquals(0, BigDecimal("1000").compareTo(assets[0].available))
    }

    @Test
    fun `placeOrder が正しくマッピングされること`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("test_api_key", request.headers["API-KEY"])

            val requestBody = (request.body as TextContent).text
            // MARKET注文時に price, timeInForce, cancelBefore が含まれないことを検証
            assertEquals(true, requestBody.contains("\"symbol\":\"BTC\""))
            assertEquals(true, requestBody.contains("\"side\":\"BUY\""))
            assertEquals(true, requestBody.contains("\"executionType\":\"MARKET\""))
            assertEquals(true, requestBody.contains("\"size\":\"0.01\""))
            assertEquals(false, requestBody.contains("\"price\""))
            assertEquals(false, requestBody.contains("\"timeInForce\""))
            assertEquals(false, requestBody.contains("\"cancelBefore\""))

            respond(
                content = """
                    {
                      "status": 0,
                      "data": "12345",
                      "responsetime": "2023-01-01T00:00:00.000Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GmoPrivateApiClientImpl(
            httpClient = HttpClient(mockEngine),
            baseUrl = baseUrl,
            signatureGenerator = signatureGenerator,
            credentialProvider = credentialProvider,
            dtoMapper = dtoMapper,
            clock = clock
        )

        val result = client.placeOrder("BTC", "BUY", "MARKET", BigDecimal("0.01"))
        assertEquals("12345", result.orderId)
    }
}
