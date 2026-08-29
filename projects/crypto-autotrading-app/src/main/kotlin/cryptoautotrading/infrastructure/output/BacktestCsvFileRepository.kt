package cryptoautotrading.infrastructure.output

import com.opencsv.CSVWriter
import cryptoautotrading.domain.backtest.BacktestResult
import cryptoautotrading.domain.repository.BacktestResultOutputPort
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileWriter

/**
 * バックテスト結果をCSVファイルに出力する実装
 */
class BacktestCsvFileRepository : BacktestResultOutputPort {

    private val logger = KotlinLogging.logger {}

    /**
     * バックテスト結果を出力する
     * @param result バックテスト結果
     * @param summaryOutputPath サマリーの保存先
     * @param stepsOutputPath ステップごとの保存先
     */
    override fun output(
        result: BacktestResult,
        summaryOutputPath: String,
        stepsOutputPath: String
    ) {
        if (summaryOutputPath.isBlank() || stepsOutputPath.isBlank()) {
            throw IllegalArgumentException("出力先パスが指定されていません")
        }

        try {
            outputSummary(result.summary, summaryOutputPath)
            outputSteps(result.steps, stepsOutputPath)
        } catch (e: Exception) {
            logger.error(e) { "バックテスト結果の出力に失敗しました。" }
            throw e
        }
    }

    /**
     * バックテストのサマリーを出力する
     * @param result バックテスト結果
     * @param outputPath 保存先パス
     */
    private fun outputSummary(summary: cryptoautotrading.domain.backtest.BacktestSummary, path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()

        CSVWriter(FileWriter(file)).use { writer ->
            // ヘッダー
            writer.writeNext(
                arrayOf(
                    "戦略名",
                    "初期資金",
                    "最終総資産",
                    "確定損益",
                    "利益率",
                    "売買回数",
                    "買い回数",
                    "売り回数",
                    "最大ドローダウン",
                    "利確回数",
                    "損切り回数",
                    "勝率",
                    "平均利益",
                    "平均損失",
                    "最大利益",
                    "最大損失",
                    "最大連続損切り回数",
                    "未決済ポジションあり",
                    "手数料率",
                    "スリッページ率"
                )
            )

            // データ行
            writer.writeNext(
                arrayOf(
                    summary.strategyName,
                    summary.initialCapital.toPlainString(),
                    summary.finalAssetValue.toPlainString(),
                    summary.realizedProfitAndLoss.toPlainString(),
                    summary.totalReturnRate.toPlainString(),
                    summary.tradeCount.toString(),
                    summary.buyCount.toString(),
                    summary.sellCount.toString(),
                    summary.maxDrawdown.toPlainString(),
                    summary.takeProfitCount.toString(),
                    summary.stopLossCount.toString(),
                    summary.winRate.toPlainString(),
                    summary.averageProfit.toPlainString(),
                    summary.averageLoss.toPlainString(),
                    summary.maxProfit.toPlainString(),
                    summary.maxLoss.toPlainString(),
                    summary.maxConsecutiveLossCount.toString(),
                    summary.hasOpenPosition.toString(),
                    summary.feeRate.toPlainString(),
                    summary.slippageRate.toPlainString()
                )
            )
        }
        logger.info { "サマリー結果を保存しました: $path" }
    }

    /**
     * バックテストの各ステップの詳細を出力する
     * @param steps ステップごとのデータ
     * @param outputPath 保存先パス
     */
    private fun outputSteps(steps: List<cryptoautotrading.domain.backtest.BacktestStepResult>, path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()

        CSVWriter(FileWriter(file)).use { writer ->
            // ヘッダー
            writer.writeNext(
                arrayOf(
                    "K線開始時刻",
                    "終値",
                    "売買判定",
                    "判定理由",
                    "現金残高",
                    "保有数量",
                    "買値",
                    "確定損益",
                    "評価額",
                    "総資産額"
                )
            )

            // データ行
            for (step in steps) {
                writer.writeNext(
                    arrayOf(
                        step.openTime,
                        step.close,
                        step.action.name,
                        step.reason,
                        step.cashBalance.toPlainString(),
                        step.holdingAmount.toPlainString(),
                        step.buyPrice.toPlainString(),
                        step.realizedProfitAndLoss.toPlainString(),
                        step.estimatedHoldingValue.toPlainString(),
                        step.totalAssetValue.toPlainString()
                    )
                )
            }
        }
        logger.info { "明細結果を保存しました: $path" }
    }
}
