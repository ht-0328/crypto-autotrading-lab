package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの資産情報取得APIのレスポンス
 *
 * @property status レスポンスステータス（例: 0 は成功）
 * @property data 資産情報のリスト
 * @property responsetime レスポンス日時
 */
@Serializable
data class GmoAssetsResponse(
    val status: Int,
    val data: List<GmoAsset>,
    val responsetime: String
)
