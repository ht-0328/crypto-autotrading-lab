package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 外部API関連の設定
 *
 * @property retryCount APIリクエスト失敗時の再試行回数
 */
data class ApiConfig(
    @JsonProperty("retry_count")
    val retryCount: Int
)
