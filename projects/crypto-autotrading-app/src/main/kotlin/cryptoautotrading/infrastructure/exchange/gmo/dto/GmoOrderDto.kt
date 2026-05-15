package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 注文リスト内の個別の注文情報DTO。
 *
 * @property rootOrderId 親注文ID
 * @property orderId 注文ID
 * @property symbol 取扱銘柄
 * @property side 売買区分
 * @property orderType 取引区分
 * @property executionType 注文タイプ
 * @property settleType 決済区分
 * @property size 発注数量
 * @property executedSize 約定数量
 * @property price 注文価格
 * @property losscutPrice ロスカットレート
 * @property status 注文ステータス
 * @property cancelType 取消区分 (キャンセル時のみ)
 * @property timeInForce 執行数量条件
 * @property timestamp 注文日時
 */
@Serializable
data class GmoOrderDto(
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
    val cancelType: String?,
    val timeInForce: String,
    val timestamp: String
)
