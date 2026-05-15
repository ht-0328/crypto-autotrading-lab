package cryptoautotrading.domain.model.order

import java.math.BigDecimal

/**
 * 取引所で完全に約定した注文を表すドメインモデル。
 *
 * @property executionId 約定ID
 * @property orderId 注文ID
 * @property symbol 銘柄
 * @property side 売買区分
 * @property actualPrice 実約定価格
 * @property actualSize 実約定数量
 * @property fee 取引手数料
 * @property timestamp 約定日時
 */
data class ExecutedOrder(
    val executionId: String,
    val orderId: String,
    val symbol: String,
    val side: String,
    val actualPrice: BigDecimal,
    val actualSize: BigDecimal,
    val fee: BigDecimal,
    val timestamp: String
)
