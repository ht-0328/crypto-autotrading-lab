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

    private fun outputSummary(summary: cryptoautotrading.domain.backtest.BacktestSummary, path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()

        CSVWriter(FileWriter(file)).use { writer ->
            // ヘッダー
            writer.writeNext(
                arrayOf(
                    "strategyName",
                    "initialCapital",
                    "finalAssetValue",
                    "realizedProfitAndLoss",
                    "totalReturnRate",
                    "tradeCount",
                    "buyCount",
                    "sellCount",
                    "maxDrawdown",
                    "takeProfitCount",
                    "stopLossCount",
                    "winRate",
                    "averageProfit",
                    "averageLoss",
                    "maxProfit",
                    "maxLoss",
                    "maxConsecutiveLossCount",
                    "hasOpenPosition"
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
                    summary.hasOpenPosition.toString()
                )
            )
        }
        logger.info { "サマリー結果を保存しました: $path" }
    }

    private fun outputSteps(steps: List<cryptoautotrading.domain.backtest.BacktestStepResult>, path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()

        CSVWriter(FileWriter(file)).use { writer ->
            // ヘッダー
            writer.writeNext(
                arrayOf(
                    "openTime",
                    "close",
                    "action",
                    "reason",
                    "cashBalance",
                    "holdingAmount",
                    "buyPrice",
                    "realizedProfitAndLoss",
                    "estimatedHoldingValue",
                    "totalAssetValue"
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
