package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの有効注文一覧取得APIのレスポンス
 *
 * @property status レスポンスステータス
 * @property data レスポンスデータ
 * @property responsetime レスポンス日時
 */
@Serializable
data class GmoActiveOrdersResponse(
    val status: Int,
    val data: GmoActiveOrdersResponseData,
    val responsetime: String
)
