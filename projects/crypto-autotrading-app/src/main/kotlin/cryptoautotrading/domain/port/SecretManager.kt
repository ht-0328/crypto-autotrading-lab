package cryptoautotrading.domain.port
interface SecretManager {
    fun getSecret(secretName: String): String
}
