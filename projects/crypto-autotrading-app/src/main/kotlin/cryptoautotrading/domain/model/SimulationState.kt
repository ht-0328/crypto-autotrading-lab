package cryptoautotrading.domain.model

import cryptoautotrading.domain.model.realtrading.RealTradingState
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * シミュレーションの状態を管理するデータクラス
 *
 * @property cashBalance 残金
 * @property isHolding 現在ポジションを保有しているかどうか
 * @property buyPrice 最後に購入したときの価格
 * @property holdingAmount 保有している数量
 * @property realizedProfitAndLoss 売却して確定した損益の累計
 * @property lastUpdatedAt 最後に状態が更新された日時（ISO 8601形式の文字列など）
 * @property lastStopLossTime 最後に損切りしたK線の時間
 * @property entryAtr エントリー時に算出されたATR。AtrTrendConfirmReboundStrategy等で使用
 * @property realTrading リアル取引固有の状態。既存stateに存在しない場合はデフォルト状態を使用する。
 */
@Serializable
data class SimulationState(
    @Serializable(with = BigDecimalSerializer::class)
    val cashBalance: BigDecimal = BigDecimal.ZERO,

    val isHolding: Boolean = false,

    @Serializable(with = BigDecimalSerializer::class)
    val buyPrice: BigDecimal = BigDecimal.ZERO,

    @Serializable(with = BigDecimalSerializer::class)
    val holdingAmount: BigDecimal = BigDecimal.ZERO,

    @Serializable(with = BigDecimalSerializer::class)
    val realizedProfitAndLoss: BigDecimal = BigDecimal.ZERO,

    val lastUpdatedAt: String = "",

    val lastStopLossTime: String = "",

    @Serializable(with = BigDecimalSerializer::class)
    val entryAtr: BigDecimal? = null,

    val realTrading: RealTradingState = RealTradingState()
)
