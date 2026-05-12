package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの注文情報取得APIのレスポンス
 *
 * @property status レスポンスステータス
 * @property data レスポンスデータ
 * @property responsetime レスポンス日時
 */
@Serializable
data class GmoOrdersResponse(
    val status: Int,
    val data: GmoOrdersResponseData,
    val responsetime: String
)
