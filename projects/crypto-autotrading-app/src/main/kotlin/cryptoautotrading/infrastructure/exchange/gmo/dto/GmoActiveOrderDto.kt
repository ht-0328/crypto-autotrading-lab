package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 有効注文リスト内の個別の注文情報DTO。
 * 現物取引では返却されないレバレッジ専用の項目（settleType, losscutPriceなど）は nullable としています。
 *
 * @property rootOrderId 親注文ID
 * @property orderId 注文ID
 * @property symbol 取扱銘柄 (現物の場合は "BTC" など)
 * @property side 売買区分 (BUY/SELL)
 * @property orderType 取引区分 (NORMAL / LOSSCUT)
 * @property executionType 注文タイプ (MARKET/LIMIT/STOP)
 * @property settleType 決済区分 (レバレッジ取引のみ、現物の場合はnull)
 * @property size 発注数量
 * @property executedSize 約定数量
 * @property price 注文価格
 * @property losscutPrice ロスカットレート (レバレッジ取引のみ、現物の場合はnull)
 * @property status 注文ステータス
 * @property timeInForce 執行数量条件
 * @property timestamp 注文日時
 */
@Serializable
data class GmoActiveOrderDto(
    val rootOrderId: Long,
    val orderId: Long,
    val symbol: String,
    val side: String,
    val orderType: String,
    val executionType: String,
    val settleType: String?,
    val size: String,
    val executedSize: String,
    val price: String,
    val losscutPrice: String?,
    val status: String,
    val timeInForce: String,
    val timestamp: String
)
