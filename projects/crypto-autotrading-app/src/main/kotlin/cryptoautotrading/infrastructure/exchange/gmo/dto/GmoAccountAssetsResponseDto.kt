package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 資産残高取得APIのレスポンスDTO。
 *
 * @property status レスポンスステータス（0が正常）
 * @property data 資産残高リスト
 * @property messages エラーメッセージリスト（エラー時のみ存在）
 * @property responsetime レスポンス時刻
 */
@Serializable
data class GmoAccountAssetsResponseDto(
    val status: Int,
    val data: List<GmoAccountAssetDto>? = null,
    val messages: List<GmoMessageDto>? = null,
    val responsetime: String
)
