package cryptoautotrading.infrastructure.output

import com.opencsv.CSVReader
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

        val klines = mutableListOf<Kline>()

        try {
            file.bufferedReader().use { reader ->
                CSVReader(reader).use { csvReader ->
                    val expectedHeader = listOf("openTime", "open", "high", "low", "close", "volume")
                    var header: Array<String>? = null
                    var lineNumber = 0

                    var nextLine: Array<String>? = csvReader.readNext()
                    while (nextLine != null) {
                        lineNumber++
                        // 空行を無視 (opencsvは空行をサイズ1の配列で空文字として返すことがある)
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
                            // データ行の処理
                            val openTime = getValue(nextLine, expectedHeader, "openTime", lineNumber)
                            val open = getValue(nextLine, expectedHeader, "open", lineNumber)
                            val high = getValue(nextLine, expectedHeader, "high", lineNumber)
                            val low = getValue(nextLine, expectedHeader, "low", lineNumber)
                            val close = getValue(nextLine, expectedHeader, "close", lineNumber)
                            val volume = getValue(nextLine, expectedHeader, "volume", lineNumber)

                            validateNumber(open, "open", lineNumber)
                            validateNumber(high, "high", lineNumber)
                            validateNumber(low, "low", lineNumber)
                            validateNumber(close, "close", lineNumber)
                            validateNumber(volume, "volume", lineNumber)

                            klines.add(Kline(openTime, open, high, low, close, volume))
                        }
                        nextLine = csvReader.readNext()
                    }

                    if (header == null) {
                        throw IllegalArgumentException("CSVファイルが空か、ヘッダーが存在しません: $inputPath")
                    }
                }
            }

            // 重複排除 (最初の行を採用) とソート
            val result = klines
                .distinctBy { it.openTime }
                .sortedBy { it.openTime.toLongOrNull() ?: 0L }

            logger.info { "過去K線データのCSV読み込みが完了しました。読み込み件数: ${result.size}" }
            return result

        } catch (e: Exception) {
            logger.error(e) { "過去K線データのCSV読み込みに失敗しました。パス: $inputPath" }
            throw e
        }
    }

    private fun getValue(row: Array<String>, header: List<String>, columnName: String, lineNumber: Int): String {
        val index = header.indexOf(columnName)
        if (index < 0 || index >= row.size) {
            throw IllegalArgumentException("CSVの${lineNumber}行目で $columnName が空です。")
        }
        val value = row[index].trim()
        if (value.isEmpty()) {
             throw IllegalArgumentException("CSVの${lineNumber}行目で $columnName が空です。")
        }
        return value
    }

    private fun validateNumber(value: String, columnName: String, lineNumber: Int) {
        try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("CSVの${lineNumber}行目で $columnName が数値として解釈できません: $value", e)
        }
    }
}
