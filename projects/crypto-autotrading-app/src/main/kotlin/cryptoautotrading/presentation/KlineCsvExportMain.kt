package cryptoautotrading.presentation

import cryptoautotrading.application.KlineCsvExportApplication
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.output.KlineCsvFileRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 過去K線CSV作成機能のエントリーポイント
 */
fun main() = runBlocking {
    logger.info { "過去K線CSV作成機能の起動処理を開始します" }

    try {
        // 環境変数からの入力値取得
        val dataDirEnv = System.getenv("APP_DATA_DIR")
        val symbolEnv = System.getenv("KLINE_EXPORT_SYMBOL")
        val intervalEnv = System.getenv("KLINE_EXPORT_INTERVAL")
        val startDateEnv = System.getenv("KLINE_EXPORT_START_DATE")
        val endDateEnv = System.getenv("KLINE_EXPORT_END_DATE")
        val outputPathEnv = System.getenv("KLINE_EXPORT_OUTPUT_PATH")

        // 出力先パスの解決
        val finalDir = if (dataDirEnv.isNullOrBlank()) {
            logger.warn { "APP_DATA_DIR が未設定です。デフォルトの './data' を使用します。" }
            "./data"
        } else {
            dataDirEnv
        }
        val resolvedOutputPath = resolveOutputPath(finalDir, outputPathEnv)

        // 設定ファイルからのAPI情報取得
        logger.info { "設定ファイルの読み込みを開始します" }
        val config = ConfigLoader.load()
        val publicBaseUrl = config.api.publicBaseUrl ?: "https://api.coin.z.com/public"
        val retryCount = config.api.retryCount
        logger.info { "採用したAPIベースURL: $publicBaseUrl, リトライ回数: $retryCount" }

        // 依存オブジェクトの生成と実行
        GmoPublicApiClient(publicBaseUrl, retryCount).use { apiClient ->
            val klineCsvRepository = KlineCsvFileRepository()
            val app = KlineCsvExportApplication(apiClient, klineCsvRepository)

            app.export(
                symbol = symbolEnv,
                interval = intervalEnv,
                startDateStr = startDateEnv,
                endDateStr = endDateEnv,
                outputPath = resolvedOutputPath
            )
        }
    } catch (e: Exception) {
        logger.error(e) { "過去K線CSV作成機能の実行中にエラーが発生しました: ${e.message}" }
        throw e
    } finally {
        logger.info { "過去K線CSV作成機能の処理が終了しました" }
    }
}

/**
 * データの保存先パスを解決する。
 * KLINE_EXPORT_OUTPUT_PATH が相対パスの場合は APP_DATA_DIR 配下として扱う。
 * KLINE_EXPORT_OUTPUT_PATH が絶対パスの場合は、そのまま使う。
 *
 * @param dataDir APP_DATA_DIR で指定されたベースディレクトリ
 * @param outputPath KLINE_EXPORT_OUTPUT_PATH で指定された出力先パス
 * @return 解決後の絶対パスまたは相対パス
 * @throws IllegalArgumentException outputPathがnullまたは空の場合
 */
internal fun resolveOutputPath(dataDir: String, outputPath: String?): String {
    if (outputPath.isNullOrBlank()) {
        throw IllegalArgumentException("KLINE_EXPORT_OUTPUT_PATH が未設定または空です。")
    }

    val path = Paths.get(outputPath)
    return if (path.isAbsolute) {
        outputPath
    } else {
        Paths.get(dataDir, outputPath).toString()
    }
}
