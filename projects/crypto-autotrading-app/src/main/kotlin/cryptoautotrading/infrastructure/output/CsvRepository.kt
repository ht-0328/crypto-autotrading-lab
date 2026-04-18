package cryptoautotrading.infrastructure.output

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 実行結果をCSVに保存するリポジトリ
 *
 * @property baseCsvFilePath 設定されたベースのCSVファイルパス
 */
class CsvRepository(private val baseCsvFilePath: String) {

    private val logger = KotlinLogging.logger {}

    /**
     * CSVファイルに実行結果を追記する。1日1ファイルになるようにファイル名を調整する。
     */
    fun append(
        datetime: String,
        price: Double,
        sign: String,
        reason: String,
        profitAndLoss: Double,
        isHolding: Boolean,
        fee: Double
    ) {
        try {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            // 例: "data/trades.csv" -> "data/trades_20231018.csv"
            val actualPath = if (baseCsvFilePath.endsWith(".csv")) {
                baseCsvFilePath.substringBeforeLast(".csv") + "_$dateStr.csv"
            } else {
                "${baseCsvFilePath}_$dateStr.csv"
            }

            val file = File(actualPath)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            val isNewFile = !file.exists()

            if (isNewFile) {
                file.appendText("日時,価格,売買サイン,理由,損益,保有状態,手数料\n")
            }

            val holdingStr = if (isHolding) "保有中" else "なし"
            file.appendText("$datetime,$price,$sign,$reason,$profitAndLoss,$holdingStr,$fee\n")

        } catch (e: Exception) {
            logger.error(e) { "Failed to write to CSV file" }
        }
    }
}
