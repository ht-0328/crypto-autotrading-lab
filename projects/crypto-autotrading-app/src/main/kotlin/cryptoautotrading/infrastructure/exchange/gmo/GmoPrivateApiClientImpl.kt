package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.order.ExchangeOrderStatus
import cryptoautotrading.domain.model.order.ExecutedOrder
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoCredentialProvider
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoSignatureGenerator
import cryptoautotrading.infrastructure.exchange.gmo.dto.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Clock

/**
 * GMO Private API クライアントの実装クラス
 *
 * @property httpClient HTTPクライアント
 * @property baseUrl APIのベースURL
 * @property signatureGenerator 署名生成器
 * @property credentialProvider 認証情報プロバイダー
 * @property dtoMapper DTOからドメインモデルへの変換器
 * @property clock タイムスタンプ取得用のClock
 */
class GmoPrivateApiClientImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val signatureGenerator: GmoSignatureGenerator,
    private val credentialProvider: GmoCredentialProvider,
    private val dtoMapper: GmoPrivateApiDtoMapper = GmoPrivateApiDtoMapper(),
    private val clock: Clock = Clock.systemUTC()
) : GmoPrivateApiClient {

    private val logger = KotlinLogging.logger {}
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * @inheritDoc
     */
    override suspend fun getAssets(): List<ExchangeAsset> {
        val path = "/v1/account/assets"
        val responseText = executeGet(path)
        logger.info { "GMO Private API raw response: $responseText" }
        return try {
            val dto = json.decodeFromString<GmoAccountAssetsResponseDto>(responseText)
            if (dto.status != 0) {
                logger.error { "GMO Private API エラーレスポンス: $responseText" }
                throw IllegalStateException("APIエラー: $responseText")
            }
            val data = dto.data ?: throw IllegalStateException("APIエラー: data が存在しません: $responseText")
            dtoMapper.mapToExchangeAssets(data)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。レスポンス本文: $responseText" }
            throw e
        }
    }

    /**
     * @inheritDoc
     */
    override suspend fun getActiveOrders(symbol: String): List<ExchangeActiveOrder> {
        val path = "/v1/activeOrders"
        val queryParams = listOf("symbol" to symbol)
        val responseText = executeGet(path, queryParams)
        logger.info { "GMO Private API raw response: $responseText" }
        return try {
            val dto = json.decodeFromString<GmoActiveOrdersResponseDto>(responseText)
            if (dto.status != 0) {
                logger.error { "GMO Private API エラーレスポンス: $responseText" }
                throw IllegalStateException("APIエラー: $responseText")
            }
            // data 自体が存在しない場合は0件として扱う
            val data = dto.data ?: return emptyList()
            // data.list が存在しない場合も0件として扱う
            val list = data.list ?: return emptyList()
            dtoMapper.mapToExchangeActiveOrders(list)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。レスポンス本文: $responseText" }
            throw e
        }
    }

    /**
     * @inheritDoc
     */
    override suspend fun placeOrder(
        symbol: String,
        side: String,
        executionType: String,
        size: BigDecimal,
        price: BigDecimal?
    ): AcceptedOrder {
        val path = "/v1/order"
        val isMarket = executionType == "MARKET"
        val requestDto = GmoPlaceOrderRequestDto(
            symbol = symbol,
            side = side,
            executionType = executionType,
            timeInForce = if (isMarket) null else "FAS", // MARKET注文では送らない
            price = if (isMarket) null else price?.toPlainString(), // MARKET注文では送らない
            size = size.toPlainString(),

            cancelBefore = null
        )
        val requestBody = json.encodeToString(requestDto)
        val responseText = executePost(path, requestBody)
        return try {
            val dto = json.decodeFromString<GmoPlaceOrderResponseDto>(responseText)
            if (dto.status != 0) {
                logger.error { "GMO Private API エラーレスポンス: $responseText" }
                throw IllegalStateException("APIエラー: $responseText")
            }
            if (dto.data == null) {
                throw IllegalStateException("APIエラー: data が存在しません: $responseText")
            }
            dtoMapper.mapToAcceptedOrder(dto)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。レスポンス本文: $responseText" }
            throw e
        }
    }

    /**
     * @inheritDoc
     */
    override suspend fun getOrders(orderId: String): List<ExchangeOrderStatus> {
        val path = "/v1/orders"
        val queryParams = listOf("orderId" to orderId)
        val responseText = executeGet(path, queryParams)
        return try {
            val dto = json.decodeFromString<GmoOrdersResponseDto>(responseText)
            if (dto.status != 0) {
                logger.error { "GMO Private API エラーレスポンス: $responseText" }
                throw IllegalStateException("APIエラー: $responseText")
            }
            val data = dto.data ?: throw IllegalStateException("APIエラー: data が存在しません: $responseText")
            dtoMapper.mapToExchangeOrderStatuses(data.list)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。レスポンス本文: $responseText" }
            throw e
        }
    }

    /**
     * @inheritDoc
     */
    override suspend fun getExecutions(orderId: String): List<ExecutedOrder> {
        val path = "/v1/executions"
        val queryParams = listOf("orderId" to orderId)
        val responseText = executeGet(path, queryParams)
        return try {
            val dto = json.decodeFromString<GmoExecutionsResponseDto>(responseText)
            if (dto.status != 0) {
                logger.error { "GMO Private API エラーレスポンス: $responseText" }
                throw IllegalStateException("APIエラー: $responseText")
            }
            val data = dto.data ?: throw IllegalStateException("APIエラー: data が存在しません: $responseText")
            dtoMapper.mapToExecutedOrders(data.list)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。レスポンス本文: $responseText" }
            throw e
        }
    }

    /**
     * GETリクエストを実行する。
     *
     * @param path APIのパス
     * @param queryParams クエリパラメータのリスト
     * @return レスポンスの文字列
     */
    private suspend fun executeGet(path: String, queryParams: List<Pair<String, String>> = emptyList()): String {
        val queryString = if (queryParams.isNotEmpty()) {
            "?" + queryParams.joinToString("&") { "${it.first}=${it.second}" }
        } else {
            ""
        }
        val fullPath = path + queryString
        val timestamp = clock.millis().toString()
        val credential = credentialProvider.getCredential()

        val sign = signatureGenerator.generate(
            timestamp = timestamp,
            method = "GET",
            path = path,
            body = "",
            secretKey = credential.secretKey
        )

        val url = "$baseUrl$fullPath"

        logger.info { "GMO Private API (GET) を呼び出します: $fullPath" }

        val response = httpClient.get(url) {
            header("API-KEY", credential.apiKey)
            header("API-TIMESTAMP", timestamp)
            header("API-SIGN", sign)
        }

        return response.bodyAsText()
    }

    /**
     * POSTリクエストを実行する。
     *
     * @param path APIのパス
     * @param bodyStr リクエストボディのJSON文字列
     * @return レスポンスの文字列
     */
    private suspend fun executePost(path: String, bodyStr: String): String {
        val timestamp = clock.millis().toString()
        val credential = credentialProvider.getCredential()

        val sign = signatureGenerator.generate(
            timestamp = timestamp,
            method = "POST",
            path = path,
            body = bodyStr,
            secretKey = credential.secretKey
        )

        val url = "$baseUrl$path"

        logger.info { "GMO Private API (POST) を呼び出します: $path" }

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            header("API-KEY", credential.apiKey)
            header("API-TIMESTAMP", timestamp)
            header("API-SIGN", sign)
            setBody(bodyStr)
        }

        return response.bodyAsText()
    }
}
