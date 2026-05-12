package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの注文詳細情報
 *
 * @property rootOrderId ルート注文ID
 * @property orderId 注文ID
 * @property symbol シンボル
 * @property side 売買区分
 * @property orderType 注文タイプ
 * @property executionType 執行数量条件
 * @property settleType 決済種別
 * @property size 注文数量
 * @property executedSize 約定数量
 * @property price 注文価格
 * @property losscutPrice ロスカット価格
 * @property status 注文ステータス
 * @property cancelType キャンセル種別
 * @property timeInForce 有効期間条件
 * @property timestamp タイムスタンプ
 */
@Serializable
data class GmoOrderData(
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
    val cancelType: String,
    val timeInForce: String,
    val timestamp: String
)

/**
 * GMOコインの注文詳細一覧
 *
 * @property list 注文詳細のリスト
 */
@Serializable
data class GmoOrdersResponseData(
    val list: List<GmoOrderData>
)

/**
 * GMOコインの注文情報取得APIのレスポンス
 *
 * @property status レスポンスステータス
 * @property data レスポンスデータ
 * @property responsetime レスポンス日時
 */
@Serializable
data class GmoOrdersResponse(
    val status: Int,
    val data: GmoOrdersResponseData,
    val responsetime: String
)
