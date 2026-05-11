package cryptoautotrading.domain.repository

/**
 * シークレット管理機能を提供するインターフェース
 */
interface SecretManager {
    /**
     * 指定された名前のシークレットを取得する
     *
     * @param secretName 取得するシークレットの名前
     * @return 取得したシークレットの値。取得に失敗した場合は例外を投げるかnullを返す
     */
    fun getSecret(secretName: String): String?
}
