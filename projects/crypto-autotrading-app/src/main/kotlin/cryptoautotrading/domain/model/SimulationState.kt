package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable

/**
 * シミュレーションの状態を管理するデータクラス
 *
 * @property isHolding 現在ポジションを保有しているかどうか
 * @property buyPrice 最後に購入したときの価格
 * @property holdingAmount 保有している数量
 * @property lastUpdatedAt 最後に状態が更新された日時（ISO 8601形式の文字列など）
 */
@Serializable
data class SimulationState(
    val isHolding: Boolean = false,
    val buyPrice: Double = 0.0,
    val holdingAmount: Double = 0.0,
    val lastUpdatedAt: String = ""
)
