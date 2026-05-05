package cryptoautotrading.domain.repository

import cryptoautotrading.domain.backtest.BacktestResult

/**
 * バックテスト結果を出力するためのポート
 */
interface BacktestResultOutputPort {
    /**
     * バックテスト結果（サマリーと明細）を指定のパスへ出力する
     *
     * @param result バックテスト結果
     * @param summaryOutputPath サマリーCSVの出力先パス
     * @param stepsOutputPath 明細CSVの出力先パス
     */
    fun output(
        result: BacktestResult,
        summaryOutputPath: String,
        stepsOutputPath: String
    )
}
