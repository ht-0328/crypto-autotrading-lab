package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの注文IDのみを含むデータ
 *
 * @property orderId 注文ID
 */
@Serializable
data class GmoOrderIdData(
    val orderId: String
)

/**
 * GMOコインの新規注文APIのレスポンス
 *
 * @property status レスポンスステータス
 * @property data 注文IDデータ。エラー時はStringになる場合があるため、現状のコードに合わせて文字列として扱う部分があるなら注意
 * @property messages エラーメッセージ等
 * @property responsetime レスポンス日時
 */
@Serializable
data class GmoOrderResponse(
    val status: Int,
    val data: String,
    val messages: List<GmoMessage>? = null,
    val responsetime: String
)
