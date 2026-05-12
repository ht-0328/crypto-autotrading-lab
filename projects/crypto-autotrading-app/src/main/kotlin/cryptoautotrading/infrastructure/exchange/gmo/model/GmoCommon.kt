package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * ページネーション情報
 *
 * @property currentPage 現在のページ
 * @property count ページあたりの件数
 */
@Serializable
data class GmoPagination(
    val currentPage: Int,
    val count: Int
)

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
