package cryptoautotrading.presentation

import cryptoautotrading.application.TradingApplication
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.output.ConsoleOutput
import cryptoautotrading.infrastructure.output.CsvRepository
import cryptoautotrading.infrastructure.output.StateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * アプリケーションのエントリーポイント
 */
fun main() = runBlocking {
    logger.info { "Crypto Auto-Trading Lab 起動処理を開始します" }

    try {
        // 設定を読み込む
        logger.info { "設定ファイルの読み込みを開始します" }
        val config = ConfigLoader.load()
        logger.info { "設定ファイルの読み込みが完了しました" }

        // データディレクトリの初期化
        val dataDirEnv = System.getenv("APP_DATA_DIR")
        val finalDir = if (dataDirEnv.isNullOrBlank()) {
            logger.warn { "APP_DATA_DIR が未設定です。デフォルトの './data' を使用します。" }
            "./data"
        } else {
            dataDirEnv
        }

        val dirFile = File(finalDir)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }

        // リポジトリの設定パス解決
        val statePath = Paths.get(finalDir, config.output.statePath).toString()
        val csvPath = Paths.get(finalDir, config.output.outputPath).toString()

        val stateRepository = StateRepository(statePath)
        val csvRepository = CsvRepository(csvPath)
        val resultOutputPort = ConsoleOutput

        // APIのベースURLを設定ファイルから取得する
        val baseUrl = config.api.baseUrl ?: "https://api.coin.z.com"
        logger.info { "最終的に採用したAPIベースURL: $baseUrl" }

        GmoPublicApiClient(baseUrl).use { apiClient ->
            val app = TradingApplication(
                config = config,
                marketDataClient = apiClient,
                stateRepository = stateRepository,
                tradeHistoryRepository = csvRepository,
                resultOutputPort = resultOutputPort
            )

            logger.info { "TradingApplication の実行を開始します" }
            app.run()
            logger.info { "TradingApplication の実行が終了しました" }
        }
    } catch (e: Exception) {
        logger.error(e) { "アプリケーションの起動・実行中に予期せぬエラーが発生しました: ${e.message}" }
        throw e
    } finally {
        logger.info { "Crypto Auto-Trading Lab 起動処理が終了しました" }
    }
}
