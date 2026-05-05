package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.Kline
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class KlineCsvFileRepositoryTest {

    private val repository = KlineCsvFileRepository()

    @Test
    fun `正常系 データを指定したパスにCSV形式で保存できること`() {
        // Arrange
        val tempDir = Files.createTempDirectory("kline-test-dir").toFile()
        val outputPath = Paths.get(tempDir.absolutePath, "test.csv").toString()
        val klines = listOf(
            Kline(openTime = "1000", open = "1.0", high = "1.5", low = "0.5", close = "1.2", volume = "100"),
            Kline(openTime = "2000", open = "2.0", high = "2.5", low = "1.5", close = "2.2", volume = "200")
        )

        // Act
        repository.save(klines, outputPath)

        // Assert
        val file = File(outputPath)
        assertTrue(file.exists())

        val lines = file.readLines()
        assertEquals(3, lines.size)
        assertEquals("openTime,open,high,low,close,volume", lines[0]) // ヘッダー
        assertEquals("1000,1.0,1.5,0.5,1.2,100", lines[1])
        assertEquals("2000,2.0,2.5,1.5,2.2,200", lines[2])

        // Cleanup
        file.delete()
        tempDir.delete()
    }

    @Test
    fun `正常系 空のリストを渡した場合、ヘッダーのみ出力されること`() {
        // Arrange
        val tempFile = File.createTempFile("kline-empty-test", ".csv")
        val outputPath = tempFile.absolutePath

        // Act
        repository.save(emptyList(), outputPath)

        // Assert
        assertTrue(tempFile.exists())
        val lines = tempFile.readLines()
        assertEquals(1, lines.size)
        assertEquals("openTime,open,high,low,close,volume", lines[0])

        // Cleanup
        tempFile.delete()
    }

    @Test
    fun `正常系 親ディレクトリが存在しない場合、作成して保存できること`() {
        // Arrange
        val tempBaseDir = Files.createTempDirectory("kline-base-dir").toFile()
        // baseDir / subdir1 / subdir2 / test.csv
        val outputPath = Paths.get(tempBaseDir.absolutePath, "subdir1", "subdir2", "test.csv").toString()
        val klines = listOf(
            Kline(openTime = "1000", open = "1.0", high = "1.5", low = "0.5", close = "1.2", volume = "100")
        )

        // Act
        repository.save(klines, outputPath)

        // Assert
        val file = File(outputPath)
        assertTrue(file.exists())
        val lines = file.readLines()
        assertEquals(2, lines.size)
        assertEquals("openTime,open,high,low,close,volume", lines[0])
        assertEquals("1000,1.0,1.5,0.5,1.2,100", lines[1])

        // Cleanup
        file.delete()
        file.parentFile.delete()
        file.parentFile.parentFile.delete()
        tempBaseDir.delete()
    }
}
