package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの有効注文データの詳細
 *
 * @property rootOrderId ルート注文ID
 * @property orderId 注文ID
 * @property symbol シンボル
 * @property side 売買区分（BUY / SELL）
 * @property orderType 注文タイプ（NORMAL など）
 * @property executionType 執行数量条件（LIMIT, MARKET など）
 * @property settleType 決済種別
 * @property size 注文数量
 * @property executedSize 約定数量
 * @property price 注文価格
 * @property losscutPrice ロスカット価格
 * @property status 注文ステータス
 * @property timeInForce 有効期間条件（FAS など）
 * @property timestamp タイムスタンプ
 */
@Serializable
data class GmoActiveOrderData(
    val rootOrderId: Long,
    val orderId: Long,
    val symbol: String,
    val side: String,
    val orderType: String,
    val executionType: String,
    val settleType: String,
    val size: String,
    val executedSize: String,
    val price: String,
    val losscutPrice: String,
    val status: String,
    val timeInForce: String,
    val timestamp: String
)
