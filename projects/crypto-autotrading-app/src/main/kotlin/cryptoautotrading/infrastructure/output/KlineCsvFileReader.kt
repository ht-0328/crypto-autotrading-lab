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

    /**
     * 指定されたパスからCSVファイルを読み込み、K線データ（Kline）のリストに変換して返す。
     *
     * ヘッダーの順序（openTime, open, high, low, close, volume）と列数の検証、各値の空文字・数値検証を行う。
     * 読み込み後は openTime を基準に重複を排除（最初の行を採用）し、openTime の昇順でソートする。
     *
     * @param inputPath 読み込むCSVファイルのパス
     * @return 検証・ソート・重複排除された Kline のリスト
     * @throws IllegalArgumentException ファイルパス未指定、ファイルが存在しない、ヘッダー不正、必須項目不足、または数値変換に失敗した場合
     */
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

    /**
     * CSVファイルのヘッダー順序および各データ行の列数を検証する。
     *
     * @param file 検証対象のCSVファイル
     * @throws IllegalArgumentException ヘッダーが不正、データ行の列数が不正、またはファイルが空の場合
     */
    private fun validateHeaderAndColumns(file: File) {
        file.bufferedReader().use { reader ->
            CSVReader(reader).use { csvReader ->
                val expectedHeader = listOf("openTime", "open", "high", "low", "close", "volume")
                var hasHeader = false
                var lineNumber = 0

                var nextLine: Array<String>? = csvReader.readNext()
                while (nextLine != null) {
                    lineNumber++

                    if (isBlankCsvLine(nextLine)) {
                        nextLine = csvReader.readNext()
                        continue
                    }

                    if (!hasHeader) {
                        validateHeader(nextLine, expectedHeader)
                        hasHeader = true
                    } else {
                        validateColumnCount(nextLine, expectedHeader.size, lineNumber)
                    }
                    nextLine = csvReader.readNext()
                }

                if (!hasHeader) {
                    throw IllegalArgumentException("CSVファイルが空か、ヘッダーが存在しません: ${file.absolutePath}")
                }
            }
        }
    }

    /**
     * CSVの行が空行（または空文字のみ）かどうかを判定する。
     *
     * @param line CSVの1行分のデータ配列
     * @return 空行であれば true、そうでなければ false
     */
    private fun isBlankCsvLine(line: Array<String>): Boolean {
        return line.isEmpty() || (line.size == 1 && line[0].isBlank())
    }

    /**
     * ヘッダー行が期待される列順序と一致するかを検証する。
     *
     * @param actualHeader 実際のヘッダー配列
     * @param expectedHeader 期待されるヘッダーのリスト
     * @throws IllegalArgumentException ヘッダーの順序が不正な場合
     */
    private fun validateHeader(actualHeader: Array<String>, expectedHeader: List<String>) {
        if (actualHeader.toList() != expectedHeader) {
            throw IllegalArgumentException("ヘッダーが openTime,open,high,low,close,volume の順番ではありません: ${actualHeader.joinToString(",")}")
        }
    }

    /**
     * データ行の列数がヘッダーの列数と一致するかを検証する。
     *
     * @param line データ行の配列
     * @param expectedSize 期待される列数
     * @param lineNumber 検証中の行番号（エラーメッセージ用）
     * @throws IllegalArgumentException 列数が不正な場合
     */
    private fun validateColumnCount(line: Array<String>, expectedSize: Int, lineNumber: Int) {
        if (line.size != expectedSize) {
            throw IllegalArgumentException("CSVの${lineNumber}行目の列数が不正です。期待値: ${expectedSize}列, 実際: ${line.size}列")
        }
    }

    /**
     * 文字列が数値（BigDecimal）として解釈できるかを検証する。
     *
     * @param value 検証対象の文字列
     * @param columnName 検証対象の列名（エラーメッセージ用）
     * @param lineNumber 検証中の行番号（エラーメッセージ用）
     * @throws IllegalArgumentException 文字列が数値として解釈できない場合
     */
    private fun validateNumber(value: String, columnName: String, lineNumber: Int) {
        try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("CSVの${lineNumber}行目で $columnName が数値として解釈できません: $value", e)
        }
    }
}
