package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの新規注文APIのレスポンス
 *
 * @property status レスポンスステータス
 * @property data 注文ID（成功時）またはエラー詳細の文字列
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
