package cryptoautotrading.infrastructure.exchange.gmo

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

/**
 * GMO API 呼び出し用の HTTP クライアントを組み立てる。
 *
 * タイムアウトを設定しないと、応答が返らないまま実行がぶら下がり続ける。
 * 定期実行のジョブでは、次の実行までに終わらない状態が積み重なる。
 */
object GmoHttpClientFactory {

    /**
     * タイムアウトを設定した HTTP クライアントを作る。
     *
     * @return 組み立てた HTTP クライアント
     */
    fun create(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        }
    }

    /** 接続の確立を待つ上限 */
    private const val CONNECT_TIMEOUT_MILLIS = 5_000L

    /** リクエスト全体の上限 */
    private const val REQUEST_TIMEOUT_MILLIS = 15_000L

    /** データを受け取る間隔の上限 */
    private const val SOCKET_TIMEOUT_MILLIS = 15_000L
}
