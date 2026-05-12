package cryptoautotrading.domain.model.order

/**
 * 注文の詳細ステータス情報
 *
 * @property orderId 注文ID
 * @property status 注文ステータス
 * @property cancelType キャンセル種別
 */
data class OrderStatusInfo(
    val orderId: Long,
    val status: String,
    val cancelType: String? = null
)
