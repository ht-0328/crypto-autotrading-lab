package cryptoautotrading.domain.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
    @Serializable(with = BigDecimalSerializer::class)
    val buyPrice: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val holdingAmount: BigDecimal = BigDecimal.ZERO,
    val lastUpdatedAt: String = ""
)
