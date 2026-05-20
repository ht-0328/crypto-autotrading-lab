package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 外部API関連の設定
 *
 * @property retryCount APIリクエスト失敗時の再試行回数
 * @property baseUrl API用のベースURL (例: https://api.coin.z.com)
 */
data class ApiConfig(
    @JsonProperty("retry_count")
    val retryCount: Int,
    @JsonProperty("base_url")
    val baseUrl: String? = null
)
