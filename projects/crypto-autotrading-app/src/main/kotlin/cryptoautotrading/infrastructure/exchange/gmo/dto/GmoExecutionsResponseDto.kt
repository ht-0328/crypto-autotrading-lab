package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 約定情報取得APIのレスポンスDTO。
 *
 * @property status レスポンスステータス
 * @property data 約定情報のリストを含むオブジェクト
 * @property responsetime レスポンス時刻
 */
@Serializable
data class GmoExecutionsResponseDto(
    val status: Int,
    val data: GmoExecutionsDataDto,
    val responsetime: String
)
