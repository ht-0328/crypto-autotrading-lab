package cryptoautotrading.domain.realtrading

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.order.ExchangeOrderStatus
import cryptoautotrading.domain.model.order.ExecutedOrder
import cryptoautotrading.domain.model.realtrading.RealOrderSide
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.realtrading.RealOrderState
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.domain.model.realtrading.RealTradingState
import cryptoautotrading.domain.realtrading.RealTradingClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RealTradingServiceTest {

    private lateinit var mockClient: MockRealTradingClient
    private lateinit var service: RealTradingService

    @BeforeEach
    fun setup() {
        mockClient = MockRealTradingClient()
        service = RealTradingService(exchangeClient = mockClient)
    }

    @Test
    fun `EXECUTEDだがgetExecutionsの合計約定数量が0の場合はisHoldingがtrueにならないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "exec_id_zero_size",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        // Mock EXECUTED response
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("exec_id_zero_size", "EXECUTED", BigDecimal("0.01")))
        // Mock executions return zero size
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder("exec_id_1", "exec_id_zero_size", "BTC", "BUY", BigDecimal("950000"), BigDecimal.ZERO, BigDecimal.ZERO, "2023-10-27T10:00:00")
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertTrue(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.UNCONFIRMED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `getOrdersがCANCELEDを返した場合、latestOrder_statusがCANCELEDになること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "canceled_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        // Mock CANCELED response
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("canceled_id", "CANCELED", BigDecimal.ZERO))

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertFalse(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.CANCELED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `getOrdersが未知のステータスを返した場合、latestOrder_statusがUNCONFIRMEDになること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "unknown_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        // Mock UNKNOWN response
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("unknown_id", "SOMETHING_NEW", BigDecimal.ZERO))

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertFalse(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.UNCONFIRMED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `realTradeEnabledがfalseの場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = false, dryRun = false)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `latestOrderがORDEREDの場合、getOrdersが呼ばれ新規placeOrderは呼ばれないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "unconfirmed_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        // Mock to return WAITING status
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("unconfirmed_id", "WAITING", BigDecimal.ZERO))

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertFalse(mockClient.placeOrderCalled)
        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.WAITING, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `getOrdersの結果がEXECUTEDの場合だけgetExecutionsが呼ばれること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "exec_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        // Mock EXECUTED response
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("exec_id", "EXECUTED", BigDecimal("0.01")))
        // Mock executions return actual value
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder("exec_id_1", "exec_id", "BTC", "BUY", BigDecimal("950000"), BigDecimal("0.01"), BigDecimal.ZERO, "2023-10-27T10:00:00")
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertTrue(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertTrue(newState.isHolding)
        assertEquals(BigDecimal("950000").compareTo(newState.buyPrice), 0)
        assertEquals(BigDecimal("0.01").compareTo(newState.holdingAmount), 0)
        assertEquals(RealOrderStatus.EXECUTED, newState.realTrading.latestOrder?.status)
        assertEquals(BigDecimal("950000").compareTo(newState.realTrading.latestOrder?.executedPrice), 0)
        assertEquals(BigDecimal("0.01").compareTo(newState.realTrading.latestOrder?.executedSize), 0)
        assertEquals("2023-10-27T10:00:00", newState.realTrading.latestOrder?.executedAt)
    }

    @Test
    fun `getExecutionsで約定情報が取れない場合はisHoldingがtrueにならないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "exec_id_no_info",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        // Mock EXECUTED response but no executions
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("exec_id_no_info", "EXECUTED", BigDecimal("0.01")))
        mockClient.mockExecutionsResponse = emptyList()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertTrue(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.UNCONFIRMED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `dryRunがtrueの場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = true)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `BUY_CANDIDATE以外の場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `exchangeClientがnullの場合は実注文処理がスキップされること`() = runBlocking {
        val serviceNullClient = RealTradingService(exchangeClient = null)
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        val newState = serviceNullClient.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
    }

    @Test
    fun `JPY残高不足の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("5000"), BigDecimal("5000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `未約定注文がある場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        mockClient.activeOrders = listOf(ExchangeActiveOrder("order1", "BTC", "BUY", BigDecimal("0.01"), BigDecimal.ZERO, "WAITING"))

        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `maxOrderJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 30000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `maxDailyOrderJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(realTrading = cryptoautotrading.domain.model.realtrading.RealTradingState(dailyOrderedJpy = BigDecimal("40000")))

        val newState = service.executeOrderIfNeeded(decision, config, 15000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `maxPositionJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(holdingAmount = BigDecimal("0.045")) // 約定価格1,000,000なら45,000円分

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `条件を満たした場合はplaceOrderが呼ばれorderIdが保存されるがisHoldingはtrueにならないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))

        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(isHolding = false)

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("BTC", mockClient.lastPlaceOrderSymbol)

        val expectedSize = BigDecimal("10000").divide(BigDecimal("1000000"), 8, java.math.RoundingMode.DOWN)
        assertEquals(expectedSize, mockClient.lastPlaceOrderSize)

        // isHolding は true にならないこと
        assertFalse(newState.isHolding)

        // latestOrder に orderId 等が保存されていること
        val latestOrder = newState.realTrading.latestOrder
        assertTrue(latestOrder != null)
        assertEquals("dummy_order_id", latestOrder?.orderId)
        assertEquals(RealOrderStatus.ORDERED, latestOrder?.status)
        assertEquals(RealOrderSide.BUY, latestOrder?.side)

        // dailyOrderedJpy が加算されていること
        assertEquals(BigDecimal("10000"), newState.realTrading.dailyOrderedJpy)
    }

    @Test
    fun `ALL_INの場合、JPY_availableを使って注文数量を計算すること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true,
            dryRun = false,
            maxOrderJpy = 20000,
            maxDailyOrderJpy = 50000,
            maxPositionJpy = 50000
        )
        val state = SimulationState()

        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("15500.5"), BigDecimal("15500.5"), BigDecimal.ONE))

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000, // tradeAmount は ALL_IN では使われない
            symbol = "BTC",
            currentState = state,
            currentPrice = BigDecimal("1000000"),
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("BTC", mockClient.lastPlaceOrderSymbol)

        // 15500.5 円を 15500 円に切り捨ててから、現在価格で割る
        val expectedSize = BigDecimal("15500").divide(BigDecimal("1000000"), 8, java.math.RoundingMode.DOWN)
        assertEquals(0, expectedSize.compareTo(mockClient.lastPlaceOrderSize))

        assertEquals(RealOrderStatus.ORDERED, newState.realTrading.latestOrder?.status)
        assertEquals("dummy_order_id", newState.realTrading.latestOrder?.orderId)
    }

    @Test
    fun `ALL_INでJPY_availableが0以下の場合は注文しないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE))

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            currentPrice = BigDecimal("1000000"),
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        assertFalse(mockClient.placeOrderCalled)
        assertEquals(null, newState.realTrading.latestOrder?.orderId)
    }

    @Test
    fun `ALL_INでmaxOrderJpyを超える場合は注文しないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true,
            dryRun = false,
            maxOrderJpy = 10000,
            maxDailyOrderJpy = 50000,
            maxPositionJpy = 50000
        )
        val state = SimulationState()

        // JPY 残高が maxOrderJpy を超えている
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("15000"), BigDecimal("15000"), BigDecimal.ONE))

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 5000, // tradeAmount は ALL_IN では使われない
            symbol = "BTC",
            currentState = state,
            currentPrice = BigDecimal("1000000"),
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        assertFalse(mockClient.placeOrderCalled)
        assertEquals(null, newState.realTrading.latestOrder?.orderId)
    }
}

class MockRealTradingClient : RealTradingClient {
    var assets: List<ExchangeAsset> = emptyList()
    var activeOrders: List<ExchangeActiveOrder> = emptyList()

    var mockOrdersResponse: List<ExchangeOrderStatus> = emptyList()
    var mockExecutionsResponse: List<ExecutedOrder> = emptyList()

    var getOrdersCalled = false
    var getExecutionsCalled = false
    var placeOrderCalled = false
    var lastPlaceOrderSymbol: String? = null
    var lastPlaceOrderSize: BigDecimal? = null

    override suspend fun getAssets(): List<ExchangeAsset> {
        return assets
    }

    override suspend fun getActiveOrders(symbol: String): List<ExchangeActiveOrder> {
        return activeOrders
    }

    override suspend fun placeOrder(
        symbol: String,
        side: String,
        executionType: String,
        size: BigDecimal,
        price: BigDecimal?
    ): AcceptedOrder {
        placeOrderCalled = true
        lastPlaceOrderSymbol = symbol
        lastPlaceOrderSize = size
        return AcceptedOrder("dummy_order_id")
    }

    override suspend fun getOrders(orderId: String): List<ExchangeOrderStatus> {
        getOrdersCalled = true
        return mockOrdersResponse
    }

    override suspend fun getExecutions(orderId: String): List<ExecutedOrder> {
        getExecutionsCalled = true
        return mockExecutionsResponse
    }
}
