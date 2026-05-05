package cryptoautotrading.application

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.KlineResponse
import cryptoautotrading.domain.repository.KlineCsvRepository
import cryptoautotrading.domain.repository.MarketDataClient
import cryptoautotrading.domain.model.TickerResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KlineCsvExportApplicationTest {

    private val marketDataClient = mockk<MarketDataClient>()
    private val klineCsvRepository = mockk<KlineCsvRepository>()
    private val app = KlineCsvExportApplication(marketDataClient, klineCsvRepository)

    @Test
    fun `正常系 必須パラメータが揃っていて、指定期間のAPIが呼ばれ、重複排除後ソートされて保存されること`() = runTest {
        // Arrange
        val symbol = "BTC"
        val interval = "5min"
        val startDateStr = "20260501"
        val endDateStr = "20260502"
        val outputPath = "dummy.csv"

        val kline1 = Kline(openTime = "1000", open = "1", high = "1", low = "1", close = "1", volume = "1")
        val kline2 = Kline(openTime = "2000", open = "2", high = "2", low = "2", close = "2", volume = "2")
        val kline2Dup = Kline(openTime = "2000", open = "2", high = "2", low = "2", close = "2", volume = "2")
        val kline3 = Kline(openTime = "3000", open = "3", high = "3", low = "3", close = "3", volume = "3")

        coEvery { marketDataClient.getKlines(symbol, interval, "20260501") } returns KlineResponse(
            status = 0,
            data = listOf(kline2, kline1), // わざと順序をバラバラにする
            responsetime = ""
        )
        coEvery { marketDataClient.getKlines(symbol, interval, "20260502") } returns KlineResponse(
            status = 0,
            data = listOf(kline2Dup, kline3), // 重複を含める
            responsetime = ""
        )

        var savedKlines: List<Kline> = emptyList()
        coEvery { klineCsvRepository.save(any(), any()) } answers {
            savedKlines = firstArg()
        }

        // Act
        app.export(symbol, interval, startDateStr, endDateStr, outputPath)

        // Assert
        coVerify(exactly = 1) { marketDataClient.getKlines(symbol, interval, "20260501") }
        coVerify(exactly = 1) { marketDataClient.getKlines(symbol, interval, "20260502") }
        verify(exactly = 1) { klineCsvRepository.save(any(), outputPath) }

        assertEquals(3, savedKlines.size)
        assertEquals("1000", savedKlines[0].openTime)
        assertEquals("2000", savedKlines[1].openTime)
        assertEquals("3000", savedKlines[2].openTime)
    }

    @Test
    fun `異常系 必須パラメータが不足している場合はIllegalArgumentExceptionを投げること`() = runTest {
        // Act & Assert
        val e = assertThrows<IllegalArgumentException> {
            app.export(null, "5min", "20260501", "20260502", "dummy.csv")
        }
        assertTrue(e.message!!.contains("必須パラメータが不足しています"))
    }

    @Test
    fun `異常系 日付の形式が不正な場合はIllegalArgumentExceptionを投げること`() = runTest {
        // Act & Assert
        val e = assertThrows<IllegalArgumentException> {
            app.export("BTC", "5min", "2026-05-01", "20260502", "dummy.csv")
        }
        assertTrue(e.message!!.contains("日付形式が不正です"))
    }

    @Test
    fun `異常系 開始日が終了日より後の場合はIllegalArgumentExceptionを投げること`() = runTest {
        // Act & Assert
        val e = assertThrows<IllegalArgumentException> {
            app.export("BTC", "5min", "20260502", "20260501", "dummy.csv")
        }
        assertTrue(e.message!!.contains("開始日が終了日より後になっています"))
    }

    @Test
    fun `異常系 API呼び出しが失敗した場合は例外をそのまま投げること`() = runTest {
        // Arrange
        coEvery { marketDataClient.getKlines(any(), any(), any()) } throws RuntimeException("API Error")

        // Act & Assert
        val e = assertThrows<RuntimeException> {
            app.export("BTC", "5min", "20260501", "20260501", "dummy.csv")
        }
        assertEquals("API Error", e.message)
    }
}
