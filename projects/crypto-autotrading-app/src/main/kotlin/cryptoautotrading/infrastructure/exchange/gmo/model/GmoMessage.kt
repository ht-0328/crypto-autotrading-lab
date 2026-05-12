package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * メッセージ情報
 *
 * @property message_code メッセージコード
 * @property message_string メッセージ本文
 */
@Serializable
data class GmoMessage(
    val message_code: String,
    val message_string: String
)
