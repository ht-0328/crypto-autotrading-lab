package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.TradeAction
import java.math.BigDecimal

/**
 * バックテスト結果の全体サマリー
 */
data class BacktestSummary(
    val strategyName: String,
    val initialCapital: BigDecimal,
    val finalAssetValue: BigDecimal,
    val realizedProfitAndLoss: BigDecimal,
    val totalReturnRate: BigDecimal,
    val tradeCount: Int,
    val buyCount: Int,
    val sellCount: Int,
    val maxDrawdown: BigDecimal
)

/**
 * バックテストにおける各ステップ（K線1本）の明細結果
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

/**
 * バックテストの全体結果（サマリーと明細のリスト）
 */
data class BacktestResult(
    val summary: BacktestSummary,
    val steps: List<BacktestStepResult>
)
