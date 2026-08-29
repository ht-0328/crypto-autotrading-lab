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
 * @property takerFeeRate 成行注文の手数料率。注文金額の上限判定に含める。
 *   未設定のまま実注文を有効にすると起動時に失敗する
 * @property maxSlippageRate 許容するスリッページの割合。次の2つに使う。
 *   1つは注文前で、K線の終値と取引所の最新価格がこの割合を超えて離れていたら注文を見送る。
 *   もう1つは約定後で、約定価格が発注時の想定価格からこの割合を超えて離れていたら新規の買いを止める。
 *   未設定のまま実注文を有効にすると起動時に失敗する
 * @property maxDailyLossJpy 1日の損失の上限（JPY、正の数で指定）。
 *   その日の確定損益がこの額のマイナスに達したら、その日は新規の買いを止める。
 *   未設定のまま実注文を有効にすると起動時に失敗する
 * @property maxConsecutiveLosses 連敗の上限。この回数に達したら、その日は新規の買いを止める。
 *   日付が変われば解除される。未設定のまま実注文を有効にすると起動時に失敗する
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
    val sizeStep: BigDecimal? = null,
    @JsonProperty("taker_fee_rate")
    val takerFeeRate: BigDecimal? = null,
    @JsonProperty("max_slippage_rate")
    val maxSlippageRate: BigDecimal? = null,
    @JsonProperty("max_daily_loss_jpy")
    val maxDailyLossJpy: Int? = null,
    @JsonProperty("max_consecutive_losses")
    val maxConsecutiveLosses: Int? = null
)
