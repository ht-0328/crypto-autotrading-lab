package cryptoautotrading.infrastructure.output

import com.opencsv.CSVReader
import com.opencsv.bean.CsvToBeanBuilder
import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.repository.KlineCsvReader
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.math.BigDecimal

/**
 * KlineCsvReader のファイルシステム向け実装。
 * 過去K線データをCSV形式のファイルから読み込む。
 */
class KlineCsvFileReader : KlineCsvReader {

    private val logger = KotlinLogging.logger {}

    override fun read(inputPath: String): List<Kline> {
        logger.info { "過去K線データのCSV読み込み処理を開始します: $inputPath" }

        if (inputPath.isBlank()) {
            throw IllegalArgumentException("CSVファイルパスが指定されていません")
        }

        val file = File(inputPath)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("CSVファイルが存在しません: $inputPath")
        }

        try {
            // ヘッダーと行数のバリデーションを先に行う
            validateHeaderAndColumns(file)

            val klines = mutableListOf<Kline>()
            file.bufferedReader().use { reader ->
                val csvReader = CSVReader(reader)
                val csvToBean = CsvToBeanBuilder<KlineCsvRow>(csvReader)
                    .withType(KlineCsvRow::class.java)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(false) // Handle exceptions manually
                    .build()

                val iterator = csvToBean.iterator()
                var lineNumber = 1 // Starting from 1 for header, so data is 2+

                while (iterator.hasNext()) {
                    lineNumber++
                    try {
                        val row = iterator.next()

                        val openTime = row.openTime?.trim() ?: throw IllegalArgumentException("CSVの${lineNumber}行目で openTime が空です。")
                        val open = row.open?.trim() ?: throw IllegalArgumentException("CSVの${lineNumber}行目で open が空です。")
                        val high = row.high?.trim() ?: throw IllegalArgumentException("CSVの${lineNumber}行目で high が空です。")
                        val low = row.low?.trim() ?: throw IllegalArgumentException("CSVの${lineNumber}行目で low が空です。")
                        val close = row.close?.trim() ?: throw IllegalArgumentException("CSVの${lineNumber}行目で close が空です。")
                        val volume = row.volume?.trim() ?: throw IllegalArgumentException("CSVの${lineNumber}行目で volume が空です。")

                        if (openTime.isEmpty()) throw IllegalArgumentException("CSVの${lineNumber}行目で openTime が空です。")
                        if (open.isEmpty()) throw IllegalArgumentException("CSVの${lineNumber}行目で open が空です。")
                        if (high.isEmpty()) throw IllegalArgumentException("CSVの${lineNumber}行目で high が空です。")
                        if (low.isEmpty()) throw IllegalArgumentException("CSVの${lineNumber}行目で low が空です。")
                        if (close.isEmpty()) throw IllegalArgumentException("CSVの${lineNumber}行目で close が空です。")
                        if (volume.isEmpty()) throw IllegalArgumentException("CSVの${lineNumber}行目で volume が空です。")

                        validateNumber(open, "open", lineNumber)
                        validateNumber(high, "high", lineNumber)
                        validateNumber(low, "low", lineNumber)
                        validateNumber(close, "close", lineNumber)
                        validateNumber(volume, "volume", lineNumber)

                        klines.add(Kline(openTime, open, high, low, close, volume))
                    } catch (e: Exception) {
                        if (e is IllegalArgumentException) {
                            throw e
                        } else {
                            throw e
                        }
                    }
                }

                // If there were captured exceptions during reading, rethrow the first one
                if (csvToBean.capturedExceptions.isNotEmpty()) {
                    val ex = csvToBean.capturedExceptions[0]
                    throw ex
                }
            }

            // 重複排除 (最初の行を採用) とソート
            val result = klines
                .distinctBy { it.openTime }
                .sortedBy { it.openTime }

            logger.info { "過去K線データのCSV読み込みが完了しました。読み込み件数: ${result.size}" }
            return result

        } catch (e: Exception) {
            logger.error(e) { "過去K線データのCSV読み込みに失敗しました。パス: $inputPath" }
            throw e
        }
    }

    private fun validateHeaderAndColumns(file: File) {
        file.bufferedReader().use { reader ->
            CSVReader(reader).use { csvReader ->
                val expectedHeader = listOf("openTime", "open", "high", "low", "close", "volume")
                var header: Array<String>? = null
                var lineNumber = 0

                var nextLine: Array<String>? = csvReader.readNext()
                while (nextLine != null) {
                    lineNumber++
                    // 空行を無視
                    if (nextLine.isEmpty() || (nextLine.size == 1 && nextLine[0].isBlank())) {
                        nextLine = csvReader.readNext()
                        continue
                    }

                    if (header == null) {
                        header = nextLine
                        if (header.toList() != expectedHeader) {
                            throw IllegalArgumentException("ヘッダーが openTime,open,high,low,close,volume の順番ではありません: ${header.joinToString(",")}")
                        }
                    } else {
                        // データ行の列数チェック
                        if (nextLine.size != expectedHeader.size) {
                            throw IllegalArgumentException("CSVの${lineNumber}行目の列数が不正です。期待値: ${expectedHeader.size}列, 実際: ${nextLine.size}列")
                        }
                    }
                    nextLine = csvReader.readNext()
                }

                if (header == null) {
                    throw IllegalArgumentException("CSVファイルが空か、ヘッダーが存在しません: ${file.absolutePath}")
                }
            }
        }
    }

    private fun validateNumber(value: String, columnName: String, lineNumber: Int) {
        try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("CSVの${lineNumber}行目で $columnName が数値として解釈できません: $value", e)
        }
    }
}
