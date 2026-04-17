package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

/**
 * K線（ローソク足）のレスポンスデータを表すクラス
 *
 * @property status レスポンスステータスコード
 * @property data K線のデータリスト
 * @property responsetime レスポンス時刻
 */
@Serializable
data class KlineResponse(
    val status: Int,
    val data: List<Kline>,
    val responsetime: String
)
