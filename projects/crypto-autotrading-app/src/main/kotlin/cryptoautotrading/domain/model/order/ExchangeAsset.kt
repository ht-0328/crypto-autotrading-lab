package cryptoautotrading.domain.model.order

import java.math.BigDecimal

/**
 * 取引所における個別の資産残高を表すドメインモデル。
 *
 * @property symbol 資産の銘柄（例: "JPY", "BTC"）
 * @property amount 資産の総残高
 * @property available 注文に利用可能な残高
 * @property conversionRate 円転レート
 */
data class ExchangeAsset(
    val symbol: String,
    val amount: BigDecimal,
    val available: BigDecimal,
    val conversionRate: BigDecimal
)
