package cryptoautotrading.infrastructure.secret

import cryptoautotrading.domain.repository.SecretManager

/**
 * 環境変数からシークレットを取得する SecretManager の実装。
 * ローカル開発環境や、テスト（Wiremock環境）での使用を想定している。
 */
class EnvVarSecretManager : SecretManager {
    /**
     * 環境変数から指定された名前のシークレットを取得する
     */
    override fun getSecret(secretName: String): String? {
        // gmo-api-key のようなハイフン区切りを環境変数用に変換する例もあるが、
        // 今回はそのまま環境変数名として取得を試みる
        return System.getenv(secretName) ?: System.getenv(secretName.replace("-", "_").uppercase())
    }
}
