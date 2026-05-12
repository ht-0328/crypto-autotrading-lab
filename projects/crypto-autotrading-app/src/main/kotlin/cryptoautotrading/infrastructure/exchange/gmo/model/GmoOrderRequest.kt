package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの新規注文APIのリクエスト
 *
 * @property symbol シンボル
 * @property side 売買区分
 * @property executionType 執行数量条件
 * @property timeInForce 有効期間条件
 * @property price 注文価格（成行の場合は省略可能だが、API仕様に合わせて文字列）
 * @property size 注文数量
 */
@Serializable
data class GmoOrderRequest(
    val symbol: String,
    val side: String,
    val executionType: String,
    val timeInForce: String,
    val price: String? = null,
    val size: String
)
