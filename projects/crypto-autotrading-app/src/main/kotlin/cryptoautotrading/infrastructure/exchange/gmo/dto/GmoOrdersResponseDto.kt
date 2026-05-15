package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 注文情報取得APIのレスポンスDTO。
 *
 * @property status レスポンスステータス
 * @property data 注文情報のリストを含むオブジェクト
 * @property responsetime レスポンス時刻
 */
@Serializable
data class GmoOrdersResponseDto(
    val status: Int,
    val data: GmoOrdersDataDto,
    val responsetime: String
)
