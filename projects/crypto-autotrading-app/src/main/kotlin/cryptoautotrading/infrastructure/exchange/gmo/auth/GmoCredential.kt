package cryptoautotrading.infrastructure.exchange.gmo.auth

/**
 * GMO Private API 認証情報を保持するデータクラス。
 *
 * @property apiKey APIキー
 * @property secretKey APIシークレットキー
 */
data class GmoCredential(
    val apiKey: String,
    val secretKey: String
)
