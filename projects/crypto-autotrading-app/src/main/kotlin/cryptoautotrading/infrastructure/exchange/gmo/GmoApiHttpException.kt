package cryptoautotrading.infrastructure.exchange.gmo

/**
 * GMO API が想定外の HTTP ステータスを返したことを表す例外。
 *
 * 再試行してよいかどうかを持たせている。混雑や一時的な障害は待てば回復するが、
 * 認証エラーや不正なリクエストは何度送っても同じ結果になるため区別する。
 *
 * @property statusCode 受け取った HTTP ステータスコード
 * @property retryable 再試行してよいかどうか
 * @property retryAfterMillis サーバーが指定した待機時間（ミリ秒）。指定が無ければ null
 */
class GmoApiHttpException(
    val statusCode: Int,
    val retryable: Boolean,
    val retryAfterMillis: Long? = null,
    message: String
) : RuntimeException(message)
