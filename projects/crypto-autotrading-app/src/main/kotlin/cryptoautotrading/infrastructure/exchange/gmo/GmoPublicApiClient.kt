package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.KlineResponse
import cryptoautotrading.domain.model.TickerResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import cryptoautotrading.domain.repository.MarketDataClient
import kotlinx.serialization.json.Json

/**
 * GMOコインパブリックAPIのクライアント
 *
 * @property baseUrl APIのベースURL
 */
class GmoPublicApiClient(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient(CIO)
) : MarketDataClient, AutoCloseable {

    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 最新のティッカー情報を取得する
     *
     * @param symbol 取得する通貨ペアのシンボル
     * @return ティッカーレスポンス
     */
    override suspend fun getTicker(symbol: String): TickerResponse {
        val url = "$baseUrl/public/v1/ticker"
        logger.info { "ティッカー情報の取得を開始します" }
        logger.debug { "APIリクエスト: GET $url?symbol=$symbol" }

        return try {
            val response = client.get(url) {
                parameter("symbol", symbol)
            }
            val statusCode = response.status.value
            val rawBody = response.bodyAsText()

            logger.debug { "APIレスポンス (HTTP $statusCode): $rawBody" }

            val decoded = json.decodeFromString<TickerResponse>(rawBody)
            logger.info { "ティッカー情報の取得が完了しました" }
            decoded
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
        val url = "$baseUrl/public/v1/klines"
        logger.info { "K線データ取得APIを呼び出します: $date" }
        logger.debug { "APIリクエスト: GET $url?symbol=$symbol&interval=$interval&date=$date" }

        return try {
            val response = client.get(url) {
                parameter("symbol", symbol)
                parameter("interval", interval)
                parameter("date", date)
            }
            val statusCode = response.status.value
            val rawBody = response.bodyAsText()

            logger.debug { "APIレスポンス本文 (HTTP $statusCode): $rawBody" }

            val decoded = json.decodeFromString<KlineResponse>(rawBody)
            logger.info { "K線データの取得が完了しました" }
            decoded
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
}
