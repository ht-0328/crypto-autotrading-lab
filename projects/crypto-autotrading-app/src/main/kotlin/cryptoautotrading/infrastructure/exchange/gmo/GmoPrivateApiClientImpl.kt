package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.order.ExchangeOrderStatus
import cryptoautotrading.domain.model.order.ExecutedOrder
import cryptoautotrading.domain.realtrading.RealTradingClient
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
) : RealTradingClient {

    private val logger = KotlinLogging.logger {}
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * APIエラーの内容を、ログや例外メッセージに出しても安全な形に要約する。
     *
     * Private API のレスポンス本文には口座残高や注文内容が含まれるため、本文そのものは含めない。
     * 原因調査に必要な、呼び出したパス・GMO のレスポンスステータス・エラーコードだけを返す。
     *
     * @param path 呼び出したAPIのパス
     * @param status GMO API のレスポンスステータス
     * @param messages GMO API のエラーメッセージリスト
     * @return 要約した文字列
     */
    private fun describeApiError(path: String, status: Int, messages: List<GmoMessageDto>?): String {
        val messageCodes = messages
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",") { it.message_code }
            ?: "なし"
        return "path=$path, status=$status, messageCodes=[$messageCodes]"
    }

    /**
     * レスポンス本文をデバッグログに出力する。
     *
     * 本文には口座残高や注文内容が含まれるため、既定のログレベル(info)では出力しない。
     * 調査時に LOG_LEVEL=debug を指定した場合のみ出力される。
     *
     * @param path 呼び出したAPIのパス
     * @param responseText レスポンス本文
     */
    private fun logResponseBodyForDebug(path: String, responseText: String) {
        logger.debug { "GMO Private API レスポンス本文 (path=$path): $responseText" }
    }

    /**
     * @inheritDoc
     */
    override suspend fun getAssets(): List<ExchangeAsset> {
        val path = "/v1/account/assets"
        val responseText = executeGet(path)
        logResponseBodyForDebug(path, responseText)
        return try {
            val dto = json.decodeFromString<GmoAccountAssetsResponseDto>(responseText)
            if (dto.status != 0) {
                val detail = describeApiError(path, dto.status, dto.messages)
                logger.error { "GMO Private API エラーレスポンス: $detail" }
                throw IllegalStateException("APIエラー: $detail")
            }
            val data = dto.data
                ?: throw IllegalStateException("APIエラー: data が存在しません: ${describeApiError(path, dto.status, dto.messages)}")
            dtoMapper.mapToExchangeAssets(data)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。path=$path" }
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
        logResponseBodyForDebug(path, responseText)
        return try {
            val dto = json.decodeFromString<GmoActiveOrdersResponseDto>(responseText)
            if (dto.status != 0) {
                val detail = describeApiError(path, dto.status, dto.messages)
                logger.error { "GMO Private API エラーレスポンス: $detail" }
                throw IllegalStateException("APIエラー: $detail")
            }
            // data 自体が存在しない場合は0件として扱う
            val data = dto.data ?: return emptyList()
            // data.list が存在しない場合も0件として扱う
            val list = data.list ?: return emptyList()
            dtoMapper.mapToExchangeActiveOrders(list)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。path=$path" }
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
        logResponseBodyForDebug(path, responseText)
        return try {
            val dto = json.decodeFromString<GmoPlaceOrderResponseDto>(responseText)
            if (dto.status != 0) {
                val detail = describeApiError(path, dto.status, dto.messages)
                logger.error { "GMO Private API エラーレスポンス: $detail" }
                throw IllegalStateException("APIエラー: $detail")
            }
            if (dto.data == null) {
                throw IllegalStateException("APIエラー: data が存在しません: ${describeApiError(path, dto.status, dto.messages)}")
            }
            dtoMapper.mapToAcceptedOrder(dto)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。path=$path" }
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
        logResponseBodyForDebug(path, responseText)
        return try {
            val dto = json.decodeFromString<GmoOrdersResponseDto>(responseText)
            if (dto.status != 0) {
                val detail = describeApiError(path, dto.status, dto.messages)
                logger.error { "GMO Private API エラーレスポンス: $detail" }
                throw IllegalStateException("APIエラー: $detail")
            }
            val data = dto.data
                ?: throw IllegalStateException("APIエラー: data が存在しません: ${describeApiError(path, dto.status, dto.messages)}")
            dtoMapper.mapToExchangeOrderStatuses(data.list)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。path=$path" }
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
        logResponseBodyForDebug(path, responseText)
        return try {
            val dto = json.decodeFromString<GmoExecutionsResponseDto>(responseText)
            if (dto.status != 0) {
                val detail = describeApiError(path, dto.status, dto.messages)
                logger.error { "GMO Private API エラーレスポンス: $detail" }
                throw IllegalStateException("APIエラー: $detail")
            }
            val data = dto.data
                ?: throw IllegalStateException("APIエラー: data が存在しません: ${describeApiError(path, dto.status, dto.messages)}")
            dtoMapper.mapToExecutedOrders(data.list)
        } catch (e: Exception) {
            logger.error(e) { "GMO Private API のレスポンスのパースまたは処理に失敗しました。path=$path" }
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

        logger.info { "GMO Private API (GET) の応答を受信しました: path=$path, httpStatus=${response.status.value}" }

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

        logger.info { "GMO Private API (POST) の応答を受信しました: path=$path, httpStatus=${response.status.value}" }

        return response.bodyAsText()
    }
}
