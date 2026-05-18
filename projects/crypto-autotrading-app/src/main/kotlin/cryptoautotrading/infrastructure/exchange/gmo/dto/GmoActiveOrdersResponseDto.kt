package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 有効注文一覧取得APIのレスポンスDTO。
 *
 * @property status レスポンスステータス
 * @property data ページネーションおよび有効注文のリスト
 * @property messages エラーメッセージリスト（エラー時のみ存在）
 * @property responsetime レスポンス時刻
 */
@Serializable
data class GmoActiveOrdersResponseDto(
    val status: Int,
    val data: GmoActiveOrdersDataDto? = null,
    val messages: List<GmoMessageDto>? = null,
    val responsetime: String
)
