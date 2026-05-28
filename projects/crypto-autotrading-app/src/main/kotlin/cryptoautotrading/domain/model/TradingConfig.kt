package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 取引関連の設定
 *
 * @property symbol 取引する通貨ペアのシンボル
 * @property initialCapital 初期資金
 * @property tradeAmount 1回の取引額
 * @property buyThreshold 買い注文を出す閾値
 * @property sellThreshold 売り注文を出す閾値
 * @property volatilityThreshold ボラティリティの閾値
 * @property sharpChangeThreshold 急変動の閾値
 * @property atrLength ATR計算に使用するK線の数
 * @property atrProfitMultiplier ATR利確倍率
 * @property atrLossMultiplier ATR損切り倍率
 */
data class TradingConfig(
    @JsonProperty("strategy_name")
    val strategyName: String = "SafeReboundStrategy",
    val symbol: String,
    @JsonProperty("initial_capital")
    val initialCapital: Int,
    @JsonProperty("trade_amount")
    val tradeAmount: Int,
    @JsonProperty("buy_threshold")
    val buyThreshold: Double,
    @JsonProperty("sell_threshold")
    val sellThreshold: Double,
    @JsonProperty("volatility_threshold")
    val volatilityThreshold: Double,
    @JsonProperty("sharp_change_threshold")
    val sharpChangeThreshold: Double,
    @JsonProperty("cooldown_length")
    val cooldownLength: Int = 12,
    @JsonProperty("atr_length")
    val atrLength: Int = 14,
    @JsonProperty("atr_profit_multiplier")
    val atrProfitMultiplier: Double = 2.0,
    @JsonProperty("atr_loss_multiplier")
    val atrLossMultiplier: Double = 2.0,
    @JsonProperty("order_sizing_mode")
    val orderSizingMode: OrderSizingMode = OrderSizingMode.FIXED_AMOUNT
)
