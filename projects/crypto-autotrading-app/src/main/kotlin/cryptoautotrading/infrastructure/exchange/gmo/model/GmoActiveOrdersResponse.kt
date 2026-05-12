package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの有効注文一覧取得APIのレスポンス内データ
 *
 * @property pagination ページネーション情報
 * @property list 有効注文のリスト
 */
@Serializable
data class GmoActiveOrdersResponseData(
    val pagination: GmoPagination,
    val list: List<GmoActiveOrderData>
)

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
