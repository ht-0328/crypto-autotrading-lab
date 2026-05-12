package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.order.ActiveOrder
import cryptoautotrading.domain.model.order.OrderSide
import cryptoautotrading.domain.model.order.OrderStatusInfo
import cryptoautotrading.domain.repository.PrivateTradingClient
import cryptoautotrading.domain.repository.SecretManager
import cryptoautotrading.infrastructure.exchange.gmo.model.*
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

/**
 * GMOコインのプライベートAPIクライアント実装。
 * HMAC-SHA256署名を用いた認証を行い、各種エンドポイントを呼び出します。
 */
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

    /**
     * リクエストヘッダーに付与するHMAC-SHA256署名を生成します。
     *
     * @param timestamp リクエストのタイムスタンプ（ミリ秒）
     * @param method HTTPメソッド
     * @param path APIエンドポイントのパス
     * @param body リクエストボディ（存在する場合）
     * @return 16進数文字列形式の署名
     */
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

    /**
     * 現在の資産情報一覧を取得します。
     *
     * @return 資産情報のリスト
     */
    override suspend fun getAssets(): List<GmoAsset> {
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
                logger.warn { "資産情報の取得に失敗しました。ステータス: ${decoded.status}" }
                return@withRetry emptyList()
            }

            decoded.data
        }
    }

    /**
     * 有効な注文一覧を取得します。
     *
     * @param symbol 検索対象のシンボル（例: BTC_JPY）
     * @return 有効注文情報のリスト
     */
    override suspend fun getActiveOrders(symbol: String): List<ActiveOrder> {
        val path = "/private/v1/activeOrders"
        val url = "$baseUrl$path"

        return withRetry {
            val (apiKey, apiSecret) = getCredentials()
            val timestamp = Instant.now().toEpochMilli().toString()
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

            val list = decoded.data.list ?: emptyList()
            list.map {
                ActiveOrder(
                    orderId = it.orderId,
                    symbol = it.symbol,
                    side = if (it.side == "BUY") OrderSide.BUY else OrderSide.SELL,
                    size = BigDecimal(it.size),
                    price = it.price.takeIf { p -> p.isNotEmpty() }?.let { p -> BigDecimal(p) },
                    status = it.status
                )
            }
        }
    }

    /**
     * 新規注文を発注します。
     *
     * @param symbol 注文対象のシンボル
     * @param side 買い/売りの別
     * @param executionType 執行条件（LIMIT, MARKET等）
     * @param timeInForce 有効期間条件
     * @param price 注文価格
     * @param size 注文数量
     * @return 発注された注文のID
     */
    override suspend fun order(
        symbol: String,
        side: OrderSide,
        executionType: String,
        timeInForce: String,
        price: BigDecimal?,
        size: BigDecimal
    ): Long {
        val path = "/private/v1/order"
        val url = "$baseUrl$path"

        return withRetry {
            val (apiKey, apiSecret) = getCredentials()
            val timestamp = Instant.now().toEpochMilli().toString()

            val requestBodyObj = GmoOrderRequest(
                symbol = symbol,
                side = side.name,
                executionType = executionType,
                timeInForce = timeInForce.takeIf { it.isNotBlank() } ?: "FAS",
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

            decoded.data.toLong()
        }
    }

    /**
     * 指定された注文IDの現在のステータスを取得します。
     *
     * @param orderId 検索対象の注文ID
     * @return 注文ステータス情報（見つからない場合はnull）
     */
    override suspend fun getOrders(orderId: Long): OrderStatusInfo? {
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
                parameter("orderId", orderId.toString())
            }

            val rawBody = response.bodyAsText()
            val decoded = json.decodeFromString<GmoOrdersResponse>(rawBody)

            if (decoded.status != 0 || decoded.data.list.isEmpty()) {
                return@withRetry null
            }

            val order = decoded.data.list.first()
            OrderStatusInfo(
                orderId = order.orderId,
                status = order.status,
                cancelType = order.cancelType
            )
        }
    }

    /**
     * HTTPクライアント等のリソースを解放します。
     */
    override fun close() {
        client.close()
    }
}
