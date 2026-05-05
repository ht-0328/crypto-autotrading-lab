package cryptoautotrading.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

class KlineCsvExportMainTest {

    @Test
    fun `正常系 相対パスの場合は APP_DATA_DIR 配下として解決されること`() {
        // Arrange
        val dataDir = "./data"
        val outputPath = "backtest/input/test.csv"

        // Act
        val result = resolveOutputPath(dataDir, outputPath)

        // Assert
        val expected = Paths.get(dataDir, outputPath).toString()
        assertEquals(expected, result)
    }

    @Test
    fun `正常系 絶対パスの場合は APP_DATA_DIR を無視してそのまま解決されること`() {
        // Arrange
        val dataDir = "./data"
        // 絶対パスの表現はOSによって異なるため、Paths.getで絶対パスを作る
        val absolutePath = Paths.get("/tmp/absolute/test.csv").toAbsolutePath().toString()

        // Act
        val result = resolveOutputPath(dataDir, absolutePath)

        // Assert
        assertEquals(absolutePath, result)
    }

    @Test
    fun `異常系 outputPath が null の場合は IllegalArgumentException を投げること`() {
        // Act & Assert
        val e = assertThrows<IllegalArgumentException> {
            resolveOutputPath("./data", null)
        }
        assertTrue(e.message!!.contains("KLINE_EXPORT_OUTPUT_PATH が未設定または空です"))
    }

    @Test
    fun `異常系 outputPath が blank の場合は IllegalArgumentException を投げること`() {
        // Act & Assert
        val e = assertThrows<IllegalArgumentException> {
            resolveOutputPath("./data", "   ")
        }
        assertTrue(e.message!!.contains("KLINE_EXPORT_OUTPUT_PATH が未設定または空です"))
    }
}
