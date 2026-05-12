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
