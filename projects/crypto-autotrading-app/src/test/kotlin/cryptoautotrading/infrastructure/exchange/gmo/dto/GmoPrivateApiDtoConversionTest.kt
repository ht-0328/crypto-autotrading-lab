package cryptoautotrading.infrastructure.exchange.gmo.dto

import cryptoautotrading.infrastructure.exchange.gmo.GmoPrivateApiClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * GMO Private API の DTO からアプリ内モデルへの変換処理をテストする。
 */
class GmoPrivateApiDtoConversionTest {

    private val client = GmoPrivateApiClient()

    @Test
    fun `GmoAccountAssetDto を ExchangeAsset に正しく変換できること`() {
        val dtoList = listOf(
            GmoAccountAssetDto(
                amount = "10000",
                available = "9500",
                conversionRate = "1.0",
                symbol = "JPY"
            )
        )

        val result = client.mapToExchangeAssets(dtoList)

        assertEquals(1, result.size)
        val asset = result[0]
        assertEquals("JPY", asset.symbol)
        assertEquals(0, BigDecimal("10000").compareTo(asset.amount))
        assertEquals(0, BigDecimal("9500").compareTo(asset.available))
        assertEquals(0, BigDecimal("1.0").compareTo(asset.conversionRate))
    }

    @Test
    fun `GmoActiveOrderDto を ExchangeActiveOrder に正しく変換できること`() {
        val dtoList = listOf(
            GmoActiveOrderDto(
                rootOrderId = 12345L,
                orderId = 67890L,
                symbol = "BTC",
                side = "BUY",
                orderType = "NORMAL",
                executionType = "LIMIT",
                settleType = "OPEN",
                size = "0.01",
                executedSize = "0.00",
                price = "5000000",
                losscutPrice = "0",
                status = "ORDERED",
                timeInForce = "FAS",
                timestamp = "2023-10-01T12:00:00Z"
            )
        )

        val result = client.mapToExchangeActiveOrders(dtoList)

        assertEquals(1, result.size)
        val order = result[0]
        assertEquals("67890", order.orderId)
        assertEquals("BTC", order.symbol)
        assertEquals("BUY", order.side)
        assertEquals(0, BigDecimal("0.01").compareTo(order.size))
        assertEquals(0, BigDecimal("0.00").compareTo(order.executedSize))
        assertEquals("ORDERED", order.status)
    }

    @Test
    fun `GmoPlaceOrderResponseDto を AcceptedOrder に正しく変換できること`() {
        val dto = GmoPlaceOrderResponseDto(
            status = 0,
            data = "98765",
            responsetime = "2023-10-01T12:05:00Z"
        )

        val result = client.mapToAcceptedOrder(dto)

        assertEquals("98765", result.orderId)
    }

    @Test
    fun `GmoOrderDto を ExchangeOrderStatus に正しく変換できること`() {
        val dtoList = listOf(
            GmoOrderDto(
                rootOrderId = 111L,
                orderId = 222L,
                symbol = "BTC",
                side = "BUY",
                orderType = "NORMAL",
                executionType = "MARKET",
                settleType = "OPEN",
                size = "0.05",
                executedSize = "0.05",
                price = "0",
                losscutPrice = "0",
                status = "EXECUTED",
                cancelType = null,
                timeInForce = "FAK",
                timestamp = "2023-10-01T12:10:00Z"
            )
        )

        val result = client.mapToExchangeOrderStatuses(dtoList)

        assertEquals(1, result.size)
        val status = result[0]
        assertEquals("222", status.orderId)
        assertEquals("EXECUTED", status.status)
        assertEquals(0, BigDecimal("0.05").compareTo(status.executedSize))
    }

    @Test
    fun `GmoExecutionDto を ExecutedOrder に正しく変換できること`() {
        val dtoList = listOf(
            GmoExecutionDto(
                executionId = 333L,
                orderId = 444L,
                positionId = null,
                symbol = "BTC",
                side = "BUY",
                settleType = "OPEN",
                size = "0.02",
                price = "5100000",
                lossGain = "0",
                fee = "10",
                timestamp = "2023-10-01T12:15:00Z"
            )
        )

        val result = client.mapToExecutedOrders(dtoList)

        assertEquals(1, result.size)
        val exec = result[0]
        assertEquals("333", exec.executionId)
        assertEquals("444", exec.orderId)
        assertEquals("BTC", exec.symbol)
        assertEquals("BUY", exec.side)
        assertEquals(0, BigDecimal("5100000").compareTo(exec.actualPrice))
        assertEquals(0, BigDecimal("0.02").compareTo(exec.actualSize))
        assertEquals(0, BigDecimal("10").compareTo(exec.fee))
        assertEquals("2023-10-01T12:15:00Z", exec.timestamp)
    }
}
