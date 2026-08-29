package cryptoautotrading.domain.model.notification

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 通知に関する設定
 *
 * **送信先のURLはここに書かない。** Webhook のURLは秘密情報なので、環境変数から渡す。
 *
 * @property enabled 通知を送るかどうか。既定は false
 * @property payloadKey 送信するJSONの本文を入れるキー。
 *   Discord は "content"、Slack は "text" を使う
 */
data class NotificationConfig(
    @JsonProperty("enabled")
    val enabled: Boolean = false,
    @JsonProperty("payload_key")
    val payloadKey: String = "content"
)
