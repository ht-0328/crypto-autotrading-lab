package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 新規注文APIのレスポンスDTO。
 *
 * @property status レスポンスステータス
 * @property data 注文受付されたorderId
 * @property messages エラーメッセージリスト（エラー時のみ存在）
 * @property responsetime レスポンス時刻
 */
@Serializable
data class GmoPlaceOrderResponseDto(
    val status: Int,
    val data: String? = null,
    val messages: List<GmoMessageDto>? = null,
    val responsetime: String
)
