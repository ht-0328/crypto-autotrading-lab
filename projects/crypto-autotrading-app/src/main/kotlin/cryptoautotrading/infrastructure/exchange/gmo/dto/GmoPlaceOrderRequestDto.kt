package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 新規注文APIのリクエストDTO。
 * 今回の初期対応は現物の成行注文（買い）のみのため、不要なレバレッジ専用項目（losscutPrice）などは省いています。
 *
 * 公式レスポンス例：
 * {
 *   "symbol": "BTC",
 *   "side": "BUY",
 *   "executionType": "MARKET",
 *   "size": "0.0001"
 * }
 *
 * @property symbol 取扱銘柄 (例: "BTC", "BTC_JPY")。現物の場合は "BTC"。
 * @property side 売買区分 (BUY/SELL)
 * @property executionType 注文タイプ (MARKET/LIMIT/STOP)
 * @property timeInForce 執行数量条件 (任意、LIMITの場合にFAK, FAS等)
 * @property price 注文価格 (MARKETの場合は不要なためnullable)
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
    val size: String,
    val cancelBefore: Boolean?
)
