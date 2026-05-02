package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.repository.TradeHistoryRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 実行結果をCSVに保存するリポジトリ
 *
 * @property baseCsvFilePath 設定されたベースのCSVファイルパス
 */
class CsvRepository(private val baseCsvFilePath: String) : TradeHistoryRepository {

    private val logger = KotlinLogging.logger {}

    /**
     * CSVファイルに実行結果を追記する。1日1ファイルになるようにファイル名を調整する。
     */
    override fun append(
        datetime: String,
        price: Double,
        sign: String,
        reason: String,
        profitAndLoss: Double,
        isHolding: Boolean,
        fee: Double
    ) {
        logger.info { "CSV保存処理を開始します" }
        try {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            // 例: "data/trades.csv" -> "data/trades_20231018.csv"
            val actualPath = if (baseCsvFilePath.endsWith(".csv")) {
                baseCsvFilePath.substringBeforeLast(".csv") + "_$dateStr.csv"
            } else {
                "${baseCsvFilePath}_$dateStr.csv"
            }

            logger.debug { "CSV保存先: $actualPath" }

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

            val escapedDatetime = escapeCsvValue(datetime)
            val escapedPrice = escapeCsvValue(price)
            val escapedSign = escapeCsvValue(sign)
            val escapedReason = escapeCsvValue(reason)
            val escapedProfitAndLoss = escapeCsvValue(profitAndLoss)
            val escapedHoldingStr = escapeCsvValue(holdingStr)
            val escapedFee = escapeCsvValue(fee)

            file.appendText("$escapedDatetime,$escapedPrice,$escapedSign,$escapedReason,$escapedProfitAndLoss,$escapedHoldingStr,$escapedFee\n")

            logger.info { "CSVへの保存が完了しました" }

        } catch (e: Exception) {
            logger.error(e) { "CSVファイルへの保存に失敗しました。パス: $baseCsvFilePath, 入力データ(datetime=$datetime, price=$price, sign=$sign, reason=$reason)" }
        }
    }

    private fun escapeCsvValue(value: Any): String {
        val str = value.toString()
        val escapedStr = str.replace("\"", "\"\"")
        return "\"$escapedStr\""
    }
}
