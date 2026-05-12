package cryptoautotrading.domain.model.order

import java.math.BigDecimal

/**
 * 有効な注文の情報
 *
 * @property orderId 注文ID
 * @property symbol シンボル
 * @property side 売買区分
 * @property size 注文数量
 * @property price 注文価格
 * @property status 注文ステータス
 */
data class ActiveOrder(
    val orderId: Long,
    val symbol: String,
    val side: OrderSide,
    val size: BigDecimal,
    val price: BigDecimal?,
    val status: String
)
