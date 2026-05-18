package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API のエラーメッセージDTO。
 *
 * @property message_code エラーコード
 * @property message_string エラーメッセージ
 */
@Serializable
data class GmoMessageDto(
    val message_code: String,
    val message_string: String
)
