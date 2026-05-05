package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.TradeAction
import java.math.BigDecimal

/**
 * K線1本ごとの結果を表すモデル。
 * 明細CSVの1行に対応する。
 *
 * @property openTime K線の開始時刻
 * @property close そのK線の終値
 * @property action 売買判定結果
 * @property reason 判定理由
 * @property cashBalance 仮想の現金残高
 * @property holdingAmount 仮想の保有数量
 * @property buyPrice 保有中の買値
 * @property realizedProfitAndLoss その時点までの確定損益
 * @property estimatedHoldingValue 保有数量を現在価格で評価した金額
 * @property totalAssetValue 現金残高と評価額を合計した総資産額
 */
data class BacktestStepResult(
    val openTime: String,
    val close: String,
    val action: TradeAction,
    val reason: String,
    val cashBalance: BigDecimal,
    val holdingAmount: BigDecimal,
    val buyPrice: BigDecimal,
    val realizedProfitAndLoss: BigDecimal,
    val estimatedHoldingValue: BigDecimal,
    val totalAssetValue: BigDecimal
)
