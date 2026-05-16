package cryptoautotrading.infrastructure.exchange.gmo.auth

/**
 * GMO Private API 用の署名を生成するインターフェース。
 */
interface GmoSignatureGenerator {
    /**
     * APIリクエストのための署名を生成する。
     *
     * @param timestamp リクエストのタイムスタンプ(ミリ秒)
     * @param method HTTPメソッド (GET, POST等)
     * @param path リクエストパス (例: /private/v1/account/assets)
     * @param body リクエストボディ（POSTなどの場合）。空の場合は空文字。
     * @param secretKey API Secret Key
     * @return HMAC-SHA256 で生成した16進数文字列の署名
     */
    fun generate(
        timestamp: String,
        method: String,
        path: String,
        body: String,
        secretKey: String
    ): String
}
