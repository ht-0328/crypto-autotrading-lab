package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 外部API関連の設定
 *
 * @property retryCount APIリクエスト失敗時の再試行回数
 * @property publicBaseUrl Public API用のベースURL (例: https://api.coin.z.com/public)
 * @property privateBaseUrl Private API用のベースURL (例: https://api.coin.z.com/private)
 */
data class ApiConfig(
    @JsonProperty("retry_count")
    val retryCount: Int,
    @JsonProperty("public_base_url")
    val publicBaseUrl: String? = null,
    @JsonProperty("private_base_url")
    val privateBaseUrl: String? = null
)
