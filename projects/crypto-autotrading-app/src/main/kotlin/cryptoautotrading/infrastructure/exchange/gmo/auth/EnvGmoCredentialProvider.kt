package cryptoautotrading.infrastructure.exchange.gmo.auth

/**
 * 環境変数からGMO認証情報を取得するプロバイダー。
 */
class EnvGmoCredentialProvider : GmoCredentialProvider {
    /**
     * @inheritDoc
     */
    override fun getCredential(): GmoCredential {
        val apiKey = System.getenv("GMO_API_KEY")
            ?: throw IllegalStateException("環境変数 GMO_API_KEY が設定されていません")
        val secretKey = System.getenv("GMO_API_SECRET")
            ?: throw IllegalStateException("環境変数 GMO_API_SECRET が設定されていません")

        return GmoCredential(apiKey, secretKey)
    }
}
