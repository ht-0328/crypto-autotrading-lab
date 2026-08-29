package cryptoautotrading.domain.backtest

import java.math.BigDecimal

/**
 * バックテストの約定コスト設定。
 *
 * 手数料とスリッページは約定価格に織り込む。
 * 買いは価格に上乗せし、売りは価格から差し引くことで、
 * 残高がマイナスにならないようにしつつコストを損益へ反映する。
 *
 * @property feeRate 約定額に対する手数料率（例: 0.0005 は 0.05%）
 * @property slippageRate 約定価格に対するスリッページ率（例: 0.0005 は 0.05%）
 */
data class BacktestCostConfig(
    val feeRate: BigDecimal = BigDecimal.ZERO,
    val slippageRate: BigDecimal = BigDecimal.ZERO
)
