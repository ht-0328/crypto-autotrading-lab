package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 資産残高リスト内の個別の資産情報DTO。
 *
 * @property amount 残高
 * @property available 利用可能金額（残高 - 出金予定額）
 * @property conversionRate 円転レート
 * @property symbol 資産残高銘柄
 */
@Serializable
data class GmoAccountAssetDto(
    val amount: String,
    val available: String,
    val conversionRate: String,
    val symbol: String
)
