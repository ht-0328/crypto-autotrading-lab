package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

/**
 * ティッカーのレスポンスデータを表すクラス
 *
 * @property status レスポンスステータスコード
 * @property data ティッカーのデータリスト
 * @property responsetime レスポンス時刻
 */
@Serializable
data class TickerResponse(
    val status: Int,
    val data: List<Ticker>,
    val responsetime: String
)
