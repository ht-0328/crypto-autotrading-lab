package cryptoautotrading.domain.model.order

/**
 * 取引所によって受け付けられた注文を表すドメインモデル。
 * 注文送信直後に発行される orderId を保持する。
 *
 * @property orderId 注文ID
 */
data class AcceptedOrder(
    val orderId: String
)
