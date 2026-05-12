package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの資産情報
 *
 * @property amount 残高
 * @property available 注文可能残高
 * @property conversionRate 変換レート
 * @property symbol シンボル（例: JPY, BTC）
 */
@Serializable
data class GmoAsset(
    val amount: String,
    val available: String,
    val conversionRate: String,
    val symbol: String
)
