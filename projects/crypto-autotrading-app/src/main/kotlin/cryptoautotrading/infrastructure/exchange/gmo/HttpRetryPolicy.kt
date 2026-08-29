package cryptoautotrading.infrastructure.exchange.gmo

import kotlin.random.Random

/**
 * HTTP 呼び出しを再試行するかどうかと、次に待つ時間を決める。
 *
 * 固定間隔で再試行すると、混雑しているサーバーに同じ間隔で叩き続けることになる。
 * 待ち時間を指数的に伸ばし、さらにばらつき（jitter）を入れて、
 * 複数の実行が同じタイミングで再試行しないようにする。
 *
 * **このポリシーは冪等な GET にだけ使うこと。** 発注のような POST を再試行すると、
 * 実際にはサーバーに届いていた注文をもう一度送り、二重注文になる。
 *
 * @property maxAttempts 最初の1回を含む最大試行回数
 * @property baseDelayMillis 1回目の再試行までの基準待ち時間
 * @property maxDelayMillis 待ち時間の上限
 * @property random ばらつきを決める乱数。テストでは固定した値を渡す
 */
class HttpRetryPolicy(
    private val maxAttempts: Int,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val maxDelayMillis: Long = DEFAULT_MAX_DELAY_MILLIS,
    private val random: Random = Random.Default
) {

    init {
        require(maxAttempts >= 1) { "maxAttempts は1以上である必要があります: $maxAttempts" }
        require(baseDelayMillis > 0) { "baseDelayMillis は正の数である必要があります: $baseDelayMillis" }
        require(maxDelayMillis >= baseDelayMillis) { "maxDelayMillis は baseDelayMillis 以上である必要があります" }
    }

    /**
     * まだ再試行してよいかを判定する。
     *
     * @param attempt これまでに試行した回数
     * @return 再試行してよい場合は true
     */
    fun canRetry(attempt: Int): Boolean = attempt < maxAttempts

    /**
     * 次の再試行までに待つ時間を返す。
     *
     * サーバーが待機時間を指定している場合は、それを優先する。
     *
     * @param attempt これまでに試行した回数（1回目の失敗なら 1）
     * @param retryAfterMillis サーバーが指定した待機時間。指定が無ければ null
     * @return 待つ時間（ミリ秒）
     */
    fun delayMillisFor(attempt: Int, retryAfterMillis: Long? = null): Long {
        if (retryAfterMillis != null && retryAfterMillis > 0) {
            return retryAfterMillis.coerceAtMost(maxDelayMillis)
        }

        val exponentialDelay = baseDelayMillis shl (attempt - 1).coerceAtMost(MAX_SHIFT)
        val cappedDelay = exponentialDelay.coerceIn(baseDelayMillis, maxDelayMillis)

        // 半分は固定、残り半分をばらつかせる。複数の実行が同じ時刻に再試行しないようにする
        val fixedPart = cappedDelay / 2
        val jitterPart = random.nextLong(cappedDelay / 2 + 1)
        return fixedPart + jitterPart
    }

    /**
     * HTTP ステータスコードから、再試行してよい失敗かどうかを判定する。
     *
     * @param statusCode HTTP ステータスコード
     * @return 待てば回復しうる失敗なら true
     */
    fun isRetryableStatus(statusCode: Int): Boolean {
        return statusCode == HTTP_REQUEST_TIMEOUT ||
            statusCode == HTTP_TOO_MANY_REQUESTS ||
            statusCode >= HTTP_SERVER_ERROR_MIN
    }

    private companion object {
        /** 1回目の再試行までの既定の待ち時間 */
        const val DEFAULT_BASE_DELAY_MILLIS = 500L

        /** 待ち時間の既定の上限 */
        const val DEFAULT_MAX_DELAY_MILLIS = 8_000L

        /** 待ち時間の計算でビットシフトを許す上限。桁あふれを防ぐ */
        const val MAX_SHIFT = 16

        /** リクエストタイムアウト */
        const val HTTP_REQUEST_TIMEOUT = 408

        /** レート制限 */
        const val HTTP_TOO_MANY_REQUESTS = 429

        /** サーバー側エラーの下限 */
        const val HTTP_SERVER_ERROR_MIN = 500
    }
}
