package cryptoautotrading.domain.model.order

import java.math.BigDecimal

/**
 * 注文の現在の状態を表すドメインモデル。
 *
 * @property orderId 注文ID
 * @property status 注文ステータス (例: WAITING, ORDERED, EXECUTED, CANCELED など)
 * @property executedSize 約定済み数量
 */
data class ExchangeOrderStatus(
    val orderId: String,
    val status: String,
    val executedSize: BigDecimal
)
