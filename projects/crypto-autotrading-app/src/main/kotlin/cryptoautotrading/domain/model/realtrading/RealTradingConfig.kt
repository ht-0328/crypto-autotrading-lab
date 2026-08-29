package cryptoautotrading.domain.model.realtrading

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * リアル取引関連の設定
 *
 * @property dryRun dry-runモード（trueの場合、API通信を行わないシミュレーション動作）
 * @property realTradeEnabled リアル取引有効フラグ（falseの場合、シミュレーション動作）
 * @property stopOnUnconfirmedOrder 未約定注文がある場合に新規注文を停止するかどうか。
 *   **現在の実装では値に関わらず常に停止する。** `RealTradingSafetyChecker` は未確認注文があれば
 *   必ず注文を見送る。`false` を有効にすると未確認注文中でも発注できてしまい、
 *   安全側に倒す方針に反するため、この動作は変えていない。`false` 指定時は起動時に警告を出す。
 * @property maxOrderJpy 1回あたりの注文金額上限（JPY）
 * @property maxDailyOrderJpy 1日あたりの累計注文金額上限（JPY）
 * @property maxPositionJpy 現在の保有金額と注文予定額の合計の最大値（JPY）
 * @property minOrderSize 取引所が定める最小注文数量。取引所の銘柄情報から確認して設定する。
 *   未設定のまま実注文を有効にすると起動時に失敗する
 * @property sizeStep 取引所が定める注文数量の刻み。注文数量はこの値の整数倍でなければ拒否される。
 *   未設定のまま実注文を有効にすると起動時に失敗する
 */
data class RealTradingConfig(
    @JsonProperty("dry_run")
    val dryRun: Boolean = true,
    @JsonProperty("real_trade_enabled")
    val realTradeEnabled: Boolean = false,
    @JsonProperty("stop_on_unconfirmed_order")
    val stopOnUnconfirmedOrder: Boolean = true,
    @JsonProperty("max_order_jpy")
    val maxOrderJpy: Int? = null,
    @JsonProperty("max_daily_order_jpy")
    val maxDailyOrderJpy: Int? = null,
    @JsonProperty("max_position_jpy")
    val maxPositionJpy: Int? = null,
    @JsonProperty("min_order_size")
    val minOrderSize: BigDecimal? = null,
    @JsonProperty("size_step")
    val sizeStep: BigDecimal? = null
)
