package cryptoautotrading.infrastructure.secret

import cryptoautotrading.domain.port.SecretManager

class EnvVarSecretManager : SecretManager {
    override fun getSecret(secretName: String): String {
        val envKey = secretName.replace("-", "_").uppercase()
        return System.getenv(envKey) ?: "DUMMY_SECRET" // mock for test
    }
}
