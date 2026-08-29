package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.KlineResponse
import cryptoautotrading.domain.model.TickerResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import cryptoautotrading.domain.repository.MarketDataClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * GMOコインパブリックAPIのクライアント
 *
 * @property baseUrl APIのベースURL
 * @property retryCount 失敗時に再試行する回数
 * @property client HTTP クライアント
 * @property retryPolicy 再試行の可否と待ち時間を決めるポリシー
 */
class GmoPublicApiClient(
    private val baseUrl: String,
    private val retryCount: Int = 0,
    private val client: HttpClient = GmoHttpClientFactory.create(),
    private val retryPolicy: HttpRetryPolicy = HttpRetryPolicy(maxAttempts = retryCount + 1)
) : MarketDataClient, AutoCloseable {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 冪等な GET の再試行を行う共通関数。
     *
     * 待てば回復しうる失敗だけを再試行する。応答の解析に失敗した場合など、
     * 何度送っても結果が変わらない失敗は、そのまま呼び出し元に返す。
     *
     * **発注のような POST にこの関数を使ってはいけない。** 実際には届いていた注文を
     * もう一度送り、二重注文になる。
     *
     * @param operation 実行する処理
     * @return 処理結果
     */
    private suspend fun <T> withRetry(operation: suspend () -> T): T {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val retryAfterMillis = (e as? GmoApiHttpException)?.retryAfterMillis
                if (!isRetryable(e) || !retryPolicy.canRetry(attempt)) {
                    throw e
                }
                val delayMillis = retryPolicy.delayMillisFor(attempt, retryAfterMillis)
                logger.warn(e) {
                    "API呼び出しに失敗しました。${delayMillis}ms 待って再試行します ($attempt/$retryCount)"
                }
                delay(delayMillis)
            }
        }
    }

    /**
     * 再試行してよい失敗かどうかを判定する。
     *
     * @param e 発生した例外
     * @return 待てば回復しうる失敗なら true
     */
    private fun isRetryable(e: Exception): Boolean {
        return when (e) {
            is GmoApiHttpException -> e.retryable
            // 通信の失敗は待てば回復しうる。解析の失敗などは何度送っても同じ
            is java.io.IOException -> true
            is kotlinx.coroutines.TimeoutCancellationException -> true
            else -> false
        }
    }

    /**
     * HTTP ステータスを検証し、想定外なら例外にする。
     *
     * 検証せずに本文を解析すると、エラーページの内容を価格データとして
     * 読もうとしたり、空の応答を正常として扱ったりする。
     *
     * @param response 受け取った応答
     * @param url 呼び出した URL
     * @throws GmoApiHttpException ステータスが成功でない場合
     */
    private fun validateHttpStatus(response: HttpResponse, url: String) {
        val statusCode = response.status.value
        if (statusCode in HTTP_SUCCESS_RANGE) {
            return
        }

        val retryable = retryPolicy.isRetryableStatus(statusCode)
        val retryAfterMillis = response.headers[RETRY_AFTER_HEADER]
            ?.toLongOrNull()
            ?.let { it * MILLIS_PER_SECOND }

        throw GmoApiHttpException(
            statusCode = statusCode,
            retryable = retryable,
            retryAfterMillis = retryAfterMillis,
            message = "GMO Public API が想定外の HTTP ステータスを返しました。url=$url, httpStatus=$statusCode"
        )
    }

    /**
     * GMO API のレスポンスステータスを検証し、成功でなければ例外にする。
     *
     * HTTP が 200 でも、API 側のステータスが 0 以外なら処理は失敗している。
     *
     * @param apiStatus GMO API のレスポンスステータス
     * @param url 呼び出した URL
     * @throws IllegalStateException ステータスが 0 以外の場合
     */
    private fun validateApiStatus(apiStatus: Int, url: String) {
        if (apiStatus != GMO_API_SUCCESS_STATUS) {
            throw IllegalStateException("GMO Public API がエラーを返しました。url=$url, status=$apiStatus")
        }
    }

    /**
     * 最新のティッカー情報を取得する
     *
     * @param symbol 取得する通貨ペアのシンボル
     * @return ティッカーレスポンス
     */
    override suspend fun getTicker(symbol: String): TickerResponse {
        val url = "$baseUrl/v1/ticker"
        logger.info { "ティッカー情報の取得を開始します" }
        logger.debug { "APIリクエスト: GET $url?symbol=$symbol" }

        return try {
            withRetry {
                val response = client.get(url) {
                    parameter("symbol", symbol)
                }
                validateHttpStatus(response, url)

                val statusCode = response.status.value
                val rawBody = response.bodyAsText()

                logger.debug { "APIレスポンス (HTTP $statusCode): $rawBody" }

                val decoded = json.decodeFromString<TickerResponse>(rawBody)
                validateApiStatus(decoded.status, url)
                logger.info { "ティッカー情報の取得が完了しました" }
                decoded
            }
        } catch (e: Exception) {
            logger.error(e) { "ティッカー情報の取得に失敗しました。URL: $url, symbol: $symbol" }
            throw e
        }
    }

    /**
     * K線（ローソク足）データを取得する
     *
     * @param symbol 取得する通貨ペアのシンボル
     * @param interval K線の間隔
     * @param date 取得する日付 (yyyyMMdd形式)
     * @return K線レスポンス
     */
    override suspend fun getKlines(symbol: String, interval: String, date: String): KlineResponse {
        val url = "$baseUrl/v1/klines"
        logger.info { "K線データ取得APIを呼び出します: $date" }
        logger.debug { "APIリクエスト: GET $url?symbol=$symbol&interval=$interval&date=$date" }

        return try {
            withRetry {
                val response = client.get(url) {
                    parameter("symbol", symbol)
                    parameter("interval", interval)
                    parameter("date", date)
                }
                validateHttpStatus(response, url)

                val statusCode = response.status.value
                val rawBody = response.bodyAsText()

                logger.debug { "APIレスポンス本文 (HTTP $statusCode): $rawBody" }

                val decoded = json.decodeFromString<KlineResponse>(rawBody)
                validateApiStatus(decoded.status, url)
                logger.info { "K線データの取得が完了しました" }
                decoded
            }
        } catch (e: Exception) {
            logger.error(e) { "K線データの取得に失敗しました。URL: $url, symbol: $symbol, interval: $interval, date: $date" }
            throw e
        }
    }

    /**
     * HTTPクライアントをクローズしてリソースを解放する
     */
    override fun close() {
        client.close()
    }

    private companion object {
        /** 成功とみなす HTTP ステータスの範囲 */
        val HTTP_SUCCESS_RANGE = 200..299

        /** サーバーが待機時間を指定するヘッダー */
        const val RETRY_AFTER_HEADER = "Retry-After"

        /** 1秒のミリ秒 */
        const val MILLIS_PER_SECOND = 1_000L

        /** GMO API が成功を表すステータス */
        const val GMO_API_SUCCESS_STATUS = 0
    }
}
