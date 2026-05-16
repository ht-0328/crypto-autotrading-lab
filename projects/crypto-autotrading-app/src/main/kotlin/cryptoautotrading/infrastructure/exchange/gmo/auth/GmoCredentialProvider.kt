package cryptoautotrading.infrastructure.exchange.gmo.auth

/**
 * GMO Private API の認証情報を提供するインターフェース。
 */
interface GmoCredentialProvider {
    /**
     * 認証情報を取得する。
     *
     * @return [GmoCredential] インスタンス
     * @throws IllegalStateException 認証情報が取得できない場合
     */
    fun getCredential(): GmoCredential
}
