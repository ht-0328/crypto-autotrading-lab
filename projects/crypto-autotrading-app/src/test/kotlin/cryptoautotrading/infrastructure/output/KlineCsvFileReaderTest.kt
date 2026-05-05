package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.Kline
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class KlineCsvFileReaderTest {

    private val reader = KlineCsvFileReader()

    @Test
    fun `read - 正常系_CSVが正しく読み込まれ_Klineのリストとして返されること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_kline.csv").toFile()
        csvFile.writeText(
            """
            "openTime","open","high","low","close","volume"
            "1717200000000","10000000","10050000","9950000","10020000","12.5"
            "1717200300000","10020000","10030000","10010000","10025000","8.2"
            """.trimIndent()
        )

        // Act
        val result = reader.read(csvFile.absolutePath)

        // Assert
        assertEquals(2, result.size)

        val kline1 = result[0]
        assertEquals("1717200000000", kline1.openTime)
        assertEquals("10000000", kline1.open)
        assertEquals("10050000", kline1.high)
        assertEquals("9950000", kline1.low)
        assertEquals("10020000", kline1.close)
        assertEquals("12.5", kline1.volume)

        val kline2 = result[1]
        assertEquals("1717200300000", kline2.openTime)
        assertEquals("10025000", kline2.close)
    }

    @Test
    fun `read - 正常系_openTimeが重複している場合_最初の行が採用されること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_duplicate.csv").toFile()
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume
            1717200000000,1000,1005,995,1002,12.5
            1717200000000,9999,9999,9999,9999,99.9
            1717200300000,1002,1003,1001,1002,8.2
            """.trimIndent()
        )

        // Act
        val result = reader.read(csvFile.absolutePath)

        // Assert
        assertEquals(2, result.size)
        val kline1 = result[0]
        assertEquals("1717200000000", kline1.openTime)
        assertEquals("1000", kline1.open) // 最初の行が採用されていることを確認
    }

    @Test
    fun `read - 正常系_openTimeの昇順にソートされること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_sort.csv").toFile()
        // 順番がバラバラのCSVを作成
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume
            1717200300000,1002,1003,1001,1002,8.2
            1717200000000,1000,1005,995,1002,12.5
            1717200600000,1003,1006,1002,1005,10.0
            """.trimIndent()
        )

        // Act
        val result = reader.read(csvFile.absolutePath)

        // Assert
        assertEquals(3, result.size)
        assertEquals("1717200000000", result[0].openTime)
        assertEquals("1717200300000", result[1].openTime)
        assertEquals("1717200600000", result[2].openTime)
    }

    @Test
    fun `read - 正常系_空行は無視されること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_empty_lines.csv").toFile()
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume

            1717200000000,1000,1005,995,1002,12.5

            1717200300000,1002,1003,1001,1002,8.2
            """.trimIndent()
        )

        // Act
        val result = reader.read(csvFile.absolutePath)

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `read - 異常系_ファイルが存在しない場合_例外がスローされること`() {
        // Arrange
        val notExistFile = "path/to/not/exist.csv"

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(notExistFile)
        }
        assertTrue(exception.message!!.contains("CSVファイルが存在しません"))
    }

    @Test
    fun `read - 異常系_パスが空の場合_例外がスローされること`() {
        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read("")
        }
        assertEquals("CSVファイルパスが指定されていません", exception.message)
    }

    @Test
    fun `read - 異常系_ヘッダーが不正な場合_例外がスローされること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_bad_header.csv").toFile()
        csvFile.writeText(
            """
            openTime,close,high,low,open,volume
            1717200000000,1002,1005,995,1000,12.5
            """.trimIndent()
        )

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(csvFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("ヘッダーが openTime,open,high,low,close,volume の順番ではありません"))
    }

    @Test
    fun `read - 異常系_必須項目が空の場合_例外がスローされること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_empty_value.csv").toFile()
        // close が空になっている (3行目)
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume
            1717200000000,1000,1005,995,1002,12.5
            1717200300000,1002,1003,1001,,8.2
            """.trimIndent()
        )

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(csvFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("CSVの3行目で close が空です。"))
    }

    @Test
    fun `read - 異常系_数値として解釈できない場合_例外がスローされること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_not_number.csv").toFile()
        // open が数値として解釈できない (3行目)
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume
            1717200000000,1000,1005,995,1002,12.5
            1717200300000,abc,1003,1001,1002,8.2
            """.trimIndent()
        )

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(csvFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("CSVの3行目で open が数値として解釈できません: abc"))
    }

    @Test
    fun `read - 異常系_データ行の列数が足りない場合にエラーになること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_missing_columns.csv").toFile()
        // 2行目のデータが5列しかない
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume
            1717200000000,1000,1005,995,1002
            """.trimIndent()
        )

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(csvFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("CSVの2行目の列数が不正です。期待値: 6列, 実際: 5列"))
    }

    @Test
    fun `read - 異常系_データ行の列数が多い場合にエラーになること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_extra_columns.csv").toFile()
        // 2行目のデータが7列ある
        csvFile.writeText(
            """
            openTime,open,high,low,close,volume
            1717200000000,1000,1005,995,1002,12.5,extra
            """.trimIndent()
        )

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(csvFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("CSVの2行目の列数が不正です。期待値: 6列, 実際: 7列"))
    }

    @Test
    fun `read - 異常系_ファイルが空の場合_例外がスローされること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("test_empty_file.csv").toFile()
        csvFile.writeText("")

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            reader.read(csvFile.absolutePath)
        }
        assertTrue(exception.message!!.contains("CSVファイルが空か、ヘッダーが存在しません"))
    }
}
