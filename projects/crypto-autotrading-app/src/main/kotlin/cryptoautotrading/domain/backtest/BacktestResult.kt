package cryptoautotrading.domain.backtest

/**
 * バックテストの結果をまとめたモデル
 *
 * @property summary バックテスト全体の成績
 * @property steps 各K線時点での明細情報
 */
data class BacktestResult(
    val summary: BacktestSummary,
    val steps: List<BacktestStepResult>
)
