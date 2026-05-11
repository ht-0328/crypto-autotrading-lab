package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.repository.ActiveOrder
import cryptoautotrading.domain.repository.OrderSide
import cryptoautotrading.domain.repository.OrderStatusInfo
import cryptoautotrading.domain.repository.PrivateTradingClient
import cryptoautotrading.domain.repository.SecretManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class GmoPrivateApiClient(
    private val baseUrl: String,
    private val retryCount: Int = 0,
    private val secretManager: SecretManager,
    private val apiKeySecretName: String,
    private val apiSecretSecretName: String,
    private val client: HttpClient = HttpClient(CIO)
) : PrivateTradingClient, AutoCloseable {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    private fun getCredentials(): Pair<String, String> {
        val apiKey = secretManager.getSecret(apiKeySecretName)
        val apiSecret = secretManager.getSecret(apiSecretSecretName)

        if (apiKey.isNullOrBlank() || apiSecret.isNullOrBlank()) {
            throw IllegalStateException("API Key or Secret is missing. Cannot call Private API.")
        }
        return Pair(apiKey, apiSecret)
    }

    private fun generateSign(timestamp: String, method: String, path: String, body: String, secret: String): String {
        val text = timestamp + method + path + body
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signBytes = mac.doFinal(text.toByteArray(Charsets.UTF_8))
        return signBytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun <T> withRetry(operation: suspend () -> T): T {
        var currentAttempt = 0
        while (true) {
            try {
                return operation()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
                currentAttempt++
                if (currentAttempt > retryCount) {
                    throw e
                }
                logger.warn(e) { "API呼び出しに失敗しました。リトライします ($currentAttempt/$retryCount)" }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    override suspend fun getAvailableBalance(symbol: String): BigDecimal {
        val path = "/private/v1/account/assets"
        val url = "$baseUrl$path"

        return withRetry {
            val (apiKey, apiSecret) = getCredentials()
            val timestamp = Instant.now().toEpochMilli().toString()
            val sign = generateSign(timestamp, "GET", path, "", apiSecret)

            val response = client.get(url) {
                header("API-KEY", apiKey)
                header("API-TIMESTAMP", timestamp)
                header("API-SIGN", sign)
            }

            val rawBody = response.bodyAsText()
            val decoded = json.decodeFromString<GmoAssetsResponse>(rawBody)

            if (decoded.status != 0) {
                throw RuntimeException("GMO API Error: status=${decoded.status}, response=$rawBody")
            }

            val asset = decoded.data.find { it.symbol == symbol }
                ?: throw RuntimeException("Asset $symbol not found in account assets.")

            BigDecimal(asset.available)
        }
    }

    override suspend fun getActiveOrders(symbol: String): List<ActiveOrder> {
        val path = "/private/v1/activeOrders"
        val url = "$baseUrl$path"

        return withRetry {
            val (apiKey, apiSecret) = getCredentials()
            val timestamp = Instant.now().toEpochMilli().toString()
            // symbol をパラメータに含める場合の署名
            val queryParams = "?symbol=$symbol"
            val sign = generateSign(timestamp, "GET", path + queryParams, "", apiSecret)

            val response = client.get(url) {
                header("API-KEY", apiKey)
                header("API-TIMESTAMP", timestamp)
                header("API-SIGN", sign)
                parameter("symbol", symbol)
            }

            val rawBody = response.bodyAsText()
            val decoded = json.decodeFromString<GmoActiveOrdersResponse>(rawBody)

            if (decoded.status != 0) {
                throw RuntimeException("GMO API Error: status=${decoded.status}, response=$rawBody")
            }

            val list = decoded.data?.list ?: emptyList()
            list.map {
                ActiveOrder(
                    orderId = it.orderId.toString(),
                    symbol = it.symbol,
                    side = if (it.side == "BUY") OrderSide.BUY else OrderSide.SELL,
                    size = BigDecimal(it.size),
                    price = it.price.takeIf { p -> p.isNotEmpty() }?.let { p -> BigDecimal(p) }
                )
            }
        }
    }

    override suspend fun placeOrder(
        symbol: String,
        side: OrderSide,
        executionType: String,
        timeInForce: String,
        price: BigDecimal?,
        size: BigDecimal
    ): String {
        val path = "/private/v1/order"
        val url = "$baseUrl$path"

        return withRetry {
            val (apiKey, apiSecret) = getCredentials()
            val timestamp = Instant.now().toEpochMilli().toString()

            val requestBodyObj = GmoOrderRequest(
                symbol = symbol,
                side = side.name,
                executionType = executionType,
                timeInForce = timeInForce.takeIf { it.isNotBlank() },
                price = price?.stripTrailingZeros()?.toPlainString(),
                size = size.stripTrailingZeros().toPlainString()
            )

            val requestBodyStr = json.encodeToString(requestBodyObj)
            val sign = generateSign(timestamp, "POST", path, requestBodyStr, apiSecret)

            val response = client.post(url) {
                header("API-KEY", apiKey)
                header("API-TIMESTAMP", timestamp)
                header("API-SIGN", sign)
                contentType(ContentType.Application.Json)
                setBody(requestBodyStr)
            }

            val rawBody = response.bodyAsText()
            val decoded = json.decodeFromString<GmoOrderResponse>(rawBody)

            if (decoded.status != 0) {
                throw RuntimeException("GMO API Order Error: status=${decoded.status}, response=$rawBody")
            }

            decoded.data
        }
    }

    override suspend fun getOrderStatus(orderId: String): OrderStatusInfo {
        val path = "/private/v1/orders"
        val url = "$baseUrl$path"

        return withRetry {
            val (apiKey, apiSecret) = getCredentials()
            val timestamp = Instant.now().toEpochMilli().toString()
            val queryParams = "?orderId=$orderId"
            val sign = generateSign(timestamp, "GET", path + queryParams, "", apiSecret)

            val response = client.get(url) {
                header("API-KEY", apiKey)
                header("API-TIMESTAMP", timestamp)
                header("API-SIGN", sign)
                parameter("orderId", orderId)
            }

            val rawBody = response.bodyAsText()
            val decoded = json.decodeFromString<GmoOrdersResponse>(rawBody)

            if (decoded.status != 0) {
                throw RuntimeException("GMO API Error: status=${decoded.status}, response=$rawBody")
            }

            val order = decoded.data?.list?.find { it.orderId.toString() == orderId }
                ?: throw RuntimeException("Order $orderId not found in orders response.")

            OrderStatusInfo(
                orderId = order.orderId.toString(),
                status = order.status, // EXECUTED, CANCELED, WAITING, etc
                executedSize = BigDecimal(order.executedSize.ifBlank { "0" }),
                executedPrice = order.price.takeIf { it.isNotBlank() }?.let { BigDecimal(it) }
            )
        }
    }

    override fun close() {
        client.close()
    }
}
