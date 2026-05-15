package cryptoautotrading.domain.model.realtrading

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * リアル取引関連の設定
 *
 * @property dryRun dry-runモード（trueの場合、API通信を行わないシミュレーション動作）
 * @property realTradeEnabled リアル取引有効フラグ（falseの場合、シミュレーション動作）
 * @property stopOnUnconfirmedOrder 未約定注文がある場合に新規注文を停止するかどうか
 * @property maxOrderJpy 1回あたりの注文金額上限（JPY）
 * @property maxDailyOrderJpy 1日あたりの累計注文金額上限（JPY）
 * @property maxPositionJpy 現在の保有金額と注文予定額の合計の最大値（JPY）
 */
data class RealTradingConfig(
    @JsonProperty("dry_run")
    val dryRun: Boolean = true,
    @JsonProperty("real_trade_enabled")
    val realTradeEnabled: Boolean = false,
    @JsonProperty("stop_on_unconfirmed_order")
    val stopOnUnconfirmedOrder: Boolean = true,
    @JsonProperty("max_order_jpy")
    val maxOrderJpy: Int?,
    @JsonProperty("max_daily_order_jpy")
    val maxDailyOrderJpy: Int?,
    @JsonProperty("max_position_jpy")
    val maxPositionJpy: Int?
)
