package cryptoautotrading.infrastructure.output

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class CsvRepositoryTest {

    @Test
    fun `ファイルが存在しない場合はヘッダーを書き込み、行を追記できること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("trades.csv").toFile()
        val repository = CsvRepository(csvFile.absolutePath)

        // Act
        repository.append(
            datetime = "2023-01-01T10:00:00",
            price = 50000.0,
            sign = "買い",
            reason = "テスト理由",
            profitAndLoss = 0.0,
            isHolding = true,
            fee = 10.0
        )

        // Assert
        val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val actualCsvFile = tempDir.resolve("trades_$dateStr.csv").toFile()

        assertTrue(actualCsvFile.exists())
        val lines = actualCsvFile.readLines()
        assertEquals(2, lines.size)
        assertEquals("日時,価格,売買サイン,理由,損益,保有状態,手数料", lines[0])
        assertEquals("\"2023-01-01T10:00:00\",\"50000.0\",\"買い\",\"テスト理由\",\"0.0\",\"保有中\",\"10.0\"", lines[1])
    }

    @Test
    fun `ファイルが存在する場合はヘッダーを書き込まずに行を追記できること`(@TempDir tempDir: Path) {
        // Arrange
        val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val actualCsvFile = tempDir.resolve("trades_$dateStr.csv").toFile()
        actualCsvFile.writeText("日時,価格,売買サイン,理由,損益,保有状態,手数料\n")

        val baseCsvFile = tempDir.resolve("trades.csv").toFile()
        val repository = CsvRepository(baseCsvFile.absolutePath)

        // Act
        repository.append(
            datetime = "2023-01-01T11:00:00",
            price = 51000.0,
            sign = "売り",
            reason = "テスト理由2",
            profitAndLoss = 1000.0,
            isHolding = false,
            fee = 15.0
        )

        // Assert
        assertTrue(actualCsvFile.exists())
        val lines = actualCsvFile.readLines()
        assertEquals(2, lines.size)
        assertEquals("日時,価格,売買サイン,理由,損益,保有状態,手数料", lines[0])
        assertEquals("\"2023-01-01T11:00:00\",\"51000.0\",\"売り\",\"テスト理由2\",\"1000.0\",\"なし\",\"15.0\"", lines[1])
    }

    @Test
    fun `カンマを含むreasonを安全に書き出せること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("trades.csv").toFile()
        val repository = CsvRepository(csvFile.absolutePath)

        // Act
        repository.append(
            datetime = "2023-01-01T12:00:00",
            price = 50000.0,
            sign = "買い",
            reason = "急落, 注意",
            profitAndLoss = 0.0,
            isHolding = true,
            fee = 10.0
        )

        // Assert
        val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val actualCsvFile = tempDir.resolve("trades_$dateStr.csv").toFile()
        val lines = actualCsvFile.readLines()
        assertEquals("\"2023-01-01T12:00:00\",\"50000.0\",\"買い\",\"急落, 注意\",\"0.0\",\"保有中\",\"10.0\"", lines[1])
    }

    @Test
    fun `ダブルクォートを含むreasonを安全に書き出せること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("trades.csv").toFile()
        val repository = CsvRepository(csvFile.absolutePath)

        // Act
        repository.append(
            datetime = "2023-01-01T12:00:00",
            price = 50000.0,
            sign = "買い",
            reason = "理由: \"急落\"",
            profitAndLoss = 0.0,
            isHolding = true,
            fee = 10.0
        )

        // Assert
        val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val actualCsvFile = tempDir.resolve("trades_$dateStr.csv").toFile()
        val lines = actualCsvFile.readLines()
        assertEquals("\"2023-01-01T12:00:00\",\"50000.0\",\"買い\",\"理由: \"\"急落\"\"\",\"0.0\",\"保有中\",\"10.0\"", lines[1])
    }

    @Test
    fun `改行を含むreasonを安全に書き出せること`(@TempDir tempDir: Path) {
        // Arrange
        val csvFile = tempDir.resolve("trades.csv").toFile()
        val repository = CsvRepository(csvFile.absolutePath)

        // Act
        repository.append(
            datetime = "2023-01-01T12:00:00",
            price = 50000.0,
            sign = "買い",
            reason = "急落\n注意",
            profitAndLoss = 0.0,
            isHolding = true,
            fee = 10.0
        )

        // Assert
        val dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        val actualCsvFile = tempDir.resolve("trades_$dateStr.csv").toFile()

        // Since there is a newline in the reason, readLines() will separate it into two elements.
        // Therefore, we just read the whole text and check the content.
        val content = actualCsvFile.readText()
        assertTrue(content.contains("\"急落\n注意\""))
        assertTrue(content.startsWith("日時,価格,売買サイン,理由,損益,保有状態,手数料\n"))
        assertTrue(content.contains("\"2023-01-01T12:00:00\",\"50000.0\",\"買い\",\"急落\n注意\",\"0.0\",\"保有中\",\"10.0\"\n"))
    }
}
