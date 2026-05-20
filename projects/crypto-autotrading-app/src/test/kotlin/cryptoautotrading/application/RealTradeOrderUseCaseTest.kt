package cryptoautotrading.application

import cryptoautotrading.application.port.RealTradingExchangePort
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.realtrading.RealOrderSide
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RealTradeOrderUseCaseTest {

    private lateinit var mockPort: MockRealTradingExchangePort
    private lateinit var useCase: RealTradeOrderUseCase

    @BeforeEach
    fun setup() {
        mockPort = MockRealTradingExchangePort()
        useCase = RealTradeOrderUseCase(exchangePort = mockPort)
    }

    @Test
    fun `realTradeEnabledがfalseの場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = false, dryRun = false)
        val state = SimulationState()

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `dryRunがtrueの場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = true)
        val state = SimulationState()

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `BUY_CANDIDATE以外の場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `exchangePortがnullの場合は実注文処理がスキップされること`() = runBlocking {
        val useCaseNullPort = RealTradeOrderUseCase(exchangePort = null)
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        val newState = useCaseNullPort.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
    }

    @Test
    fun `JPY残高不足の場合は実注文がスキップされること`() = runBlocking {
        mockPort.assets = listOf(ExchangeAsset("JPY", BigDecimal("5000"), BigDecimal("5000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `未約定注文がある場合は実注文がスキップされること`() = runBlocking {
        mockPort.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        mockPort.activeOrders = listOf(ExchangeActiveOrder("order1", "BTC", "BUY", BigDecimal("0.01"), BigDecimal.ZERO, "WAITING"))

        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `maxOrderJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockPort.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = useCase.executeOrderIfNeeded(decision, config, 30000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `maxDailyOrderJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockPort.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(realTrading = cryptoautotrading.domain.model.realtrading.RealTradingState(dailyOrderedJpy = BigDecimal("40000")))

        val newState = useCase.executeOrderIfNeeded(decision, config, 15000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `maxPositionJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockPort.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(holdingAmount = BigDecimal("0.045")) // 約定価格1,000,000なら45,000円分

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockPort.placeOrderCalled)
    }

    @Test
    fun `条件を満たした場合はplaceOrderが呼ばれorderIdが保存されるがisHoldingはtrueにならないこと`() = runBlocking {
        mockPort.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))

        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(isHolding = false)

        val newState = useCase.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"))

        assertTrue(mockPort.placeOrderCalled)
        assertEquals("BTC", mockPort.lastPlaceOrderSymbol)

        val expectedSize = BigDecimal("10000").divide(BigDecimal("1000000"), 8, java.math.RoundingMode.DOWN)
        assertEquals(expectedSize, mockPort.lastPlaceOrderSize)

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
}

class MockRealTradingExchangePort : RealTradingExchangePort {
    var assets: List<ExchangeAsset> = emptyList()
    var activeOrders: List<ExchangeActiveOrder> = emptyList()

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
}
