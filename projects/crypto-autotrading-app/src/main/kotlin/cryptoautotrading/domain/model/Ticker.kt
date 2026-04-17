package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

/**
 * ティッカーのデータを表すクラス
 *
 * @property ask 買値
 * @property bid 売値
 * @property high 高値
 * @property last 最終取引価格
 * @property low 安値
 * @property symbol 通貨ペアのシンボル
 * @property timestamp タイムスタンプ
 * @property volume 取引高
 */
@Serializable
data class Ticker(
    val ask: String,
    val bid: String,
    val high: String,
    val last: String,
    val low: String,
    val symbol: String,
    val timestamp: String,
    val volume: String
)
