package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 約定リスト内の個別の約定情報DTO。
 *
 * @property executionId 約定ID
 * @property orderId 注文ID
 * @property positionId 建玉ID (レバレッジのみ)
 * @property symbol 取扱銘柄
 * @property side 売買区分
 * @property settleType 決済区分
 * @property size 約定数量
 * @property price 約定レート
 * @property lossGain 決済損益
 * @property fee 取引手数料
 * @property timestamp 約定日時
 */
@Serializable
data class GmoExecutionDto(
    val executionId: Long,
    val orderId: Long,
    val positionId: Long?,
    val symbol: String,
    val side: String,
    val settleType: String,
    val size: String,
    val price: String,
    val lossGain: String,
    val fee: String,
    val timestamp: String
)
