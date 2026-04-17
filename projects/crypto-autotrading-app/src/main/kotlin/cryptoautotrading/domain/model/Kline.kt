package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

/**
 * K線（ローソク足）のデータを表すクラス
 *
 * @property openTime 開始時刻
 * @property open 始値
 * @property high 高値
 * @property low 安値
 * @property close 終値
 * @property volume 取引高
 */
@Serializable
data class Kline(
    val openTime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String
)
