package cryptoautotrading.infrastructure.exchange.gmo.auth

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * [GmoSignatureGenerator] の実装クラス。
 */
class GmoSignatureGeneratorImpl : GmoSignatureGenerator {
    /**
     * @inheritDoc
     */
    override fun generate(
        timestamp: String,
        method: String,
        path: String,
        body: String,
        secretKey: String
    ): String {
        val text = timestamp + method + path + body
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
