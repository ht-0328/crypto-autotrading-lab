package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.KlineResponse
import cryptoautotrading.domain.model.TickerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * GMOコインパブリックAPIのクライアント
 *
 * @property baseUrl APIのベースURL
 */
class GmoPublicApiClient(private val baseUrl: String) : AutoCloseable {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * 最新のティッカー情報を取得する
     *
     * @param symbol 取得する通貨ペアのシンボル
     * @return ティッカーレスポンス
     */
    suspend fun getTicker(symbol: String): TickerResponse {
        return client.get("$baseUrl/public/v1/ticker") {
            parameter("symbol", symbol)
        }.body()
    }

    /**
     * K線（ローソク足）データを取得する
     *
     * @param symbol 取得する通貨ペアのシンボル
     * @param interval K線の間隔
     * @param date 取得する日付 (yyyyMMdd形式)
     * @return K線レスポンス
     */
    suspend fun getKlines(symbol: String, interval: String, date: String): KlineResponse {
        return client.get("$baseUrl/public/v1/klines") {
            parameter("symbol", symbol)
            parameter("interval", interval)
            parameter("date", date)
        }.body()
    }

    /**
     * HTTPクライアントをクローズしてリソースを解放する
     */
    override fun close() {
        client.close()
    }
}
