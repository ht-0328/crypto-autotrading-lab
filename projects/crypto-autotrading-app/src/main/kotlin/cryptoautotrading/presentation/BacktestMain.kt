package cryptoautotrading.presentation

import cryptoautotrading.application.BacktestApplication
import cryptoautotrading.infrastructure.output.BacktestCsvFileRepository
import cryptoautotrading.infrastructure.output.KlineCsvFileReader
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * バックテスト実行のエントリーポイント
 */
fun main() {
    logger.info { "バックテストプロセスを開始します。" }

    try {
        val klineCsvPath = System.getenv("BACKTEST_KLINE_CSV_PATH")
        val strategyName = System.getenv("BACKTEST_STRATEGY_NAME")
        val initialCapitalStr = System.getenv("BACKTEST_INITIAL_CAPITAL")
        val summaryOutputPath = System.getenv("BACKTEST_SUMMARY_OUTPUT_PATH")
        val stepsOutputPath = System.getenv("BACKTEST_STEPS_OUTPUT_PATH")

        // 設定を読み込む。APP_CONFIG_PATH が未指定、またはファイルがない場合は
        // ConfigLoader 内でデフォルト設定にフォールバックされる
        val appConfig = cryptoautotrading.infrastructure.config.ConfigLoader.load()

        val klineCsvReader = KlineCsvFileReader()
        val resultOutputPort = BacktestCsvFileRepository()

        val application = BacktestApplication(klineCsvReader, resultOutputPort, appConfig.trading)

        application.run(
            klineCsvPath = klineCsvPath,
            strategyName = strategyName,
            initialCapitalStr = initialCapitalStr,
            summaryOutputPath = summaryOutputPath,
            stepsOutputPath = stepsOutputPath
        )

        logger.info { "バックテストプロセスが正常に終了しました。" }
        exitProcess(0)
    } catch (e: Exception) {
        logger.error(e) { "バックテストプロセスで予期せぬエラーが発生しました: ${e.message}" }
        exitProcess(1)
    }
}
