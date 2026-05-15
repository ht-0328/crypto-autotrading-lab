package cryptoautotrading.domain.model.order

import java.math.BigDecimal

/**
 * 取引所に存在する未約定注文（アクティブオーダー）を表すドメインモデル。
 *
 * @property orderId 取引所での注文ID
 * @property symbol 銘柄
 * @property side 売買区分 (BUY/SELL)
 * @property size 発注数量
 * @property executedSize 約定済み数量
 * @property status 注文のステータス
 */
data class ExchangeActiveOrder(
    val orderId: String,
    val symbol: String,
    val side: String,
    val size: BigDecimal,
    val executedSize: BigDecimal,
    val status: String
)
