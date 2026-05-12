package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの注文詳細一覧
 *
 * @property list 注文詳細のリスト
 */
@Serializable
data class GmoOrdersResponseData(
    val list: List<GmoOrderData>
)
