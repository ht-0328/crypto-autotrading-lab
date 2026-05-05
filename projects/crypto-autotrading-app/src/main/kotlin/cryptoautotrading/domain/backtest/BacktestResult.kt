package cryptoautotrading.domain.backtest

/**
 * BacktestEngine の実行結果。
 * BacktestSummary と BacktestStepResult の一覧をまとめたモデル。
 *
 * @property summary バックテスト全体のサマリー
 * @property steps 各時点（K線ごと）の明細のリスト
 */
data class BacktestResult(
    val summary: BacktestSummary,
    val steps: List<BacktestStepResult>
)
