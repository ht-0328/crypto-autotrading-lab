package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 新規注文APIのリクエストDTO。
 *
 * @property symbol 取扱銘柄
 * @property side 売買区分 (BUY/SELL)
 * @property executionType 注文タイプ (MARKET/LIMIT/STOP)
 * @property timeInForce 執行数量条件 (任意、LIMITの場合設定可能)
 * @property price 注文価格 (MARKETの場合は不要)
 * @property losscutPrice ロスカットレート (任意)
 * @property size 注文数量 (BTC等の数量)
 * @property cancelBefore 有効注文取消フラグ (任意)
 */
@Serializable
data class GmoPlaceOrderRequestDto(
    val symbol: String,
    val side: String,
    val executionType: String,
    val timeInForce: String?,
    val price: String?,
    val losscutPrice: String?,
    val size: String,
    val cancelBefore: Boolean?
)
