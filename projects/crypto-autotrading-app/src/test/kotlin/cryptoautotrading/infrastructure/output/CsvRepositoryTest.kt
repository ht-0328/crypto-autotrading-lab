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
        assertEquals("2023-01-01T10:00:00,50000.0,買い,テスト理由,0.0,保有中,10.0", lines[1])
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
        assertEquals("2023-01-01T11:00:00,51000.0,売り,テスト理由2,1000.0,なし,15.0", lines[1])
    }
}
