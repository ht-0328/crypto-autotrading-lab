package cryptoautotrading.infrastructure.exchange.gmo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cryptoautotrading.domain.model.order.*
import cryptoautotrading.domain.port.ExchangeOrderClient
import cryptoautotrading.infrastructure.exchange.gmo.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.jackson.jackson
import java.math.BigDecimal
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class GmoPrivateApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson { }
        }
    }
) : ExchangeOrderClient {
    private val logger = KotlinLogging.logger {}
    private val mapper: ObjectMapper = jacksonObjectMapper()

    override suspend fun getAssets(apiKey: String, apiSecret: String): AssetsResponse {
        val path = "/private/v1/account/assets"
        val response: GmoAssetsResponse = get(path, apiKey, apiSecret)

        if (response.status != 0) {
            throw IllegalStateException("API Error")
        }

        val mapped = response.data?.map {
            Asset(it.symbol, BigDecimal(it.available))
        } ?: emptyList()

        return AssetsResponse(mapped)
    }

    override suspend fun getActiveOrders(apiKey: String, apiSecret: String, symbol: String): ActiveOrdersResponse {
        val path = "/private/v1/activeOrders?symbol=\$symbol"
        val response: GmoActiveOrdersResponse = get(path, apiKey, apiSecret)

        if (response.status != 0) {
            throw IllegalStateException("API Error")
        }

        val mapped = response.data?.list?.map {
            ActiveOrder(it.orderId.toString(), it.symbol, it.side)
        } ?: emptyList()

        return ActiveOrdersResponse(mapped)
    }

    override suspend fun placeOrder(apiKey: String, apiSecret: String, request: OrderRequest): OrderResponse {
        val path = "/private/v1/order"
        val gmoReq = GmoOrderRequest(
            symbol = request.symbol,
            side = request.side,
            executionType = request.executionType,
            timeInForce = request.timeInForce,
            price = request.price?.toPlainString(),
            size = request.size.toPlainString()
        )

        val response: GmoOrderResponse = post(path, apiKey, apiSecret, gmoReq)

        if (response.status != 0 || response.data == null) {
            throw IllegalStateException("API Error")
        }

        return OrderResponse(response.data)
    }

    override suspend fun getExecutions(apiKey: String, apiSecret: String, orderId: String): ExecutionsResponse {
        val path = "/private/v1/executions?orderId=\$orderId"
        val response: GmoExecutionsResponse = get(path, apiKey, apiSecret)

        if (response.status != 0) {
            throw IllegalStateException("API Error")
        }

        val mapped = response.data?.list?.map {
            Execution(it.executionId.toString(), it.orderId.toString(), BigDecimal(it.price), BigDecimal(it.size))
        } ?: emptyList()

        return ExecutionsResponse(mapped)
    }

    private suspend inline fun <reified T> get(path: String, apiKey: String, apiSecret: String): T {
        val timestamp = System.currentTimeMillis().toString()
        val text = timestamp + "GET" + path
        val sign = createSignature(text, apiSecret)

        val url = "\$baseUrl\$path"

        return httpClient.get(url) {
            header("API-KEY", apiKey)
            header("API-TIMESTAMP", timestamp)
            header("API-SIGN", sign)
        }.body()
    }

    private suspend inline fun <reified T> post(path: String, apiKey: String, apiSecret: String, body: Any): T {
        val timestamp = System.currentTimeMillis().toString()
        val bodyStr = mapper.writeValueAsString(body)
        val text = timestamp + "POST" + path + bodyStr
        val sign = createSignature(text, apiSecret)

        val url = "\$baseUrl\$path"

        return httpClient.post(url) {
            contentType(ContentType.Application.Json)
            header("API-KEY", apiKey)
            header("API-TIMESTAMP", timestamp)
            header("API-SIGN", sign)
            setBody(bodyStr)
        }.body()
    }

    private fun createSignature(text: String, secretKey: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val signData = mac.doFinal(text.toByteArray())
        return signData.joinToString("") { "%02x".format(it) }
    }
}
