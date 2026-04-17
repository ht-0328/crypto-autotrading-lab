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
 */
data class TradingConfig(
    val symbol: String,
    @JsonProperty("initial_capital")
    val initialCapital: Int,
    @JsonProperty("trade_amount")
    val tradeAmount: Int,
    @JsonProperty("buy_threshold")
    val buyThreshold: Double,
    @JsonProperty("sell_threshold")
    val sellThreshold: Double
)
