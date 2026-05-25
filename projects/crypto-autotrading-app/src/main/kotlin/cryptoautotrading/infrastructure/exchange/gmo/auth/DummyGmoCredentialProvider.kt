package cryptoautotrading.infrastructure.exchange.gmo.auth

/**
 * WireMock や CI 用のダミー認証情報を返すプロバイダー。
 *
 * 本物の GMO API には使用しない。
 */
class DummyGmoCredentialProvider : GmoCredentialProvider {
    /**
     * ダミーの認証情報を取得する。
     *
     * @return ダミーの [GmoCredential]
     */
    override fun getCredential(): GmoCredential {
        return GmoCredential(
            apiKey = "dummy-api-key",
            secretKey = "dummy-api-secret"
        )
    }
}
