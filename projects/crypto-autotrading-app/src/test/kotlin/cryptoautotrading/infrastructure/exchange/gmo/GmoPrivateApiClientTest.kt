package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.order.OrderSide
import cryptoautotrading.domain.repository.SecretManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GmoPrivateApiClientTest {

    private val mockSecretManager = object : SecretManager {
        override fun getSecret(secretName: String): String? {
            return when (secretName) {
                "test-api-key" -> "dummy_api_key"
                "test-api-secret" -> "dummy_api_secret"
                else -> null
            }
        }
    }

    private fun createMockClient(mockEngine: MockEngine): GmoPrivateApiClient {
        val httpClient = HttpClient(mockEngine)
        return GmoPrivateApiClient(
            baseUrl = "http://localhost",
            retryCount = 0,
            secretManager = mockSecretManager,
            apiKeySecretName = "test-api-key",
            apiSecretSecretName = "test-api-secret",
            client = httpClient
        )
    }

    @Test
    fun `getAssets should return JPY balance successfully`() = runTest {
        val mockResponse = """
            {
              "status": 0,
              "data": [
                {
                  "amount": "10000",
                  "available": "5000",
                  "conversionRate": "1",
                  "symbol": "JPY"
                }
              ],
              "responsetime": "2023-01-01T00:00:00.000Z"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/private/v1/account/assets", request.url.encodedPath)
            assertEquals("dummy_api_key", request.headers["API-KEY"])
            assertTrue(request.headers.contains("API-TIMESTAMP"))
            assertTrue(request.headers.contains("API-SIGN"))
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = createMockClient(mockEngine)
        val assets = client.getAssets()
        val jpyBalance = assets.find { it.symbol == "JPY" }?.available?.let { BigDecimal(it) } ?: BigDecimal.ZERO
        assertEquals(0, BigDecimal("5000").compareTo(jpyBalance))
    }

    @Test
    fun `placeOrder should submit order and return orderId`() = runTest {
        val mockResponse = """
            {
              "status": 0,
              "data": "1234567890",
              "responsetime": "2023-01-01T00:00:00.000Z"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/private/v1/order", request.url.encodedPath)
            assertEquals("POST", request.method.value)
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = createMockClient(mockEngine)
        val orderId = client.order(
            symbol = "BTC_JPY",
            side = OrderSide.BUY,
            executionType = "LIMIT",
            timeInForce = "FAS",
            price = BigDecimal("1000000"),
            size = BigDecimal("0.01")
        )

        assertEquals(1234567890L, orderId)
    }

    @Test
    fun `getActiveOrders should parse correctly`() = runTest {
        val mockResponse = """
            {
              "status": 0,
              "data": {
                "pagination": { "currentPage": 1, "count": 1 },
                "list": [
                  {
                    "rootOrderId": 123456,
                    "orderId": 987654321,
                    "symbol": "BTC_JPY",
                    "side": "BUY",
                    "orderType": "NORMAL",
                    "executionType": "LIMIT",
                    "settleType": "OPEN",
                    "size": "0.1",
                    "executedSize": "0",
                    "price": "9000000",
                    "losscutPrice": "",
                    "status": "ORDERED",
                    "timeInForce": "FAS",
                    "timestamp": "2023-01-01T00:00:00.000Z"
                  }
                ]
              },
              "responsetime": "2023-01-01T00:00:00.000Z"
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("/private/v1/activeOrders", request.url.encodedPath)
            assertEquals("BTC_JPY", request.url.parameters["symbol"])
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = createMockClient(mockEngine)
        val orders = client.getActiveOrders("BTC_JPY")

        assertEquals(1, orders.size)
        assertEquals(987654321L, orders[0].orderId)
        assertEquals(OrderSide.BUY, orders[0].side)
        assertEquals(0, BigDecimal("0.1").compareTo(orders[0].size))
        assertEquals(0, BigDecimal("9000000").compareTo(orders[0].price!!))
    }

    @Test
    fun `API errors should throw RuntimeException`() = runTest {
        val mockResponse = """
            {
              "status": 1,
              "data": [],
              "responsetime": "2023-01-01T00:00:00.000Z"
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = createMockClient(mockEngine)

        val assets = client.getAssets()
        assertTrue(assets.isEmpty())
    }
}
