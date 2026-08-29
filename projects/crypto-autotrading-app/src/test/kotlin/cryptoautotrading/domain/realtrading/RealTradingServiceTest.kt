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
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
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

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertTrue(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.UNCONFIRMED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `getOrdersがCANCELEDを返した場合、latestOrder_statusがCANCELEDになること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
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

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertFalse(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.CANCELED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `getOrdersが未知のステータスを返した場合、latestOrder_statusがUNCONFIRMEDになること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
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

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertFalse(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.UNCONFIRMED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `realTradeEnabledがfalseの場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = false, dryRun = false)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `latestOrderがORDEREDの場合、getOrdersが呼ばれ新規placeOrderは呼ばれないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
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

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertFalse(mockClient.placeOrderCalled)
        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.WAITING, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `getOrdersの結果がEXECUTEDの場合だけgetExecutionsが呼ばれること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
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

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

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
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
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

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.getOrdersCalled)
        assertTrue(mockClient.getExecutionsCalled)
        assertFalse(mockClient.placeOrderCalled)

        assertFalse(newState.isHolding)
        assertEquals(RealOrderStatus.UNCONFIRMED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `dryRunがtrueの場合は実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = true)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `保有していない状態のSELL_CANDIDATEでは実注文処理がスキップされること`() = runBlocking {
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `exchangeClientがnullの場合は実注文処理がスキップされること`() = runBlocking {
        val serviceNullClient = RealTradingService(exchangeClient = null)
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        val newState = serviceNullClient.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
    }

    @Test
    fun `JPY残高不足の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("5000"), BigDecimal("5000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `未約定注文がある場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        mockClient.activeOrders = listOf(ExchangeActiveOrder("order1", "BTC", "BUY", BigDecimal("0.01"), BigDecimal.ZERO, "WAITING"))

        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `maxOrderJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 30000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `maxDailyOrderJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(realTrading = cryptoautotrading.domain.model.realtrading.RealTradingState(dailyOrderedJpy = BigDecimal("40000")))

        val newState = service.executeOrderIfNeeded(decision, config, 15000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `maxPositionJpy超過の場合は実注文がスキップされること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("50000"), BigDecimal("50000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(holdingAmount = BigDecimal("0.045")) // 約定価格1,000,000なら45,000円分

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertEquals(state, newState)
        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `条件を満たした場合はplaceOrderが呼ばれorderIdが保存されるがisHoldingはtrueにならないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))

        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(isHolding = false)

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("BTC", mockClient.lastPlaceOrderSymbol)

        // 10000円 ÷ 1,000,000円 = 0.01。刻み(0.00001)の整数倍なので丸めても値は変わらない
        assertEquals(0, BigDecimal("0.01").compareTo(mockClient.lastPlaceOrderSize))

        // isHolding は true にならないこと
        assertFalse(newState.isHolding)

        // latestOrder に orderId 等が保存されていること
        val latestOrder = newState.realTrading.latestOrder
        assertTrue(latestOrder != null)
        assertEquals("dummy_order_id", latestOrder?.orderId)
        assertEquals(RealOrderStatus.ORDERED, latestOrder?.status)
        assertEquals(RealOrderSide.BUY, latestOrder?.side)

        // dailyOrderedJpy が手数料込みの額で加算されていること
        assertEquals(0, BigDecimal("10005").compareTo(newState.realTrading.dailyOrderedJpy))
    }

    @Test
    fun `ALL_INの場合、JPY_availableを使って注文数量を計算すること`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
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
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = BigDecimal("1000000"),
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("BTC", mockClient.lastPlaceOrderSymbol)

        // 15500.5 円の残高から手数料(0.05%)分を差し引いた 15492 円を注文額とし、現在価格で割る。
        // 残高を全額注文に回すと手数料の分だけ足りなくなるため。
        // 15492 ÷ 1,000,000 = 0.015492 を刻み(0.00001)に切り捨てて 0.01549 になる
        assertEquals("0.01549", mockClient.lastPlaceOrderSize?.toPlainString())

        assertEquals(RealOrderStatus.ORDERED, newState.realTrading.latestOrder?.status)
        assertEquals("dummy_order_id", newState.realTrading.latestOrder?.orderId)
    }

    @Test
    fun `ALL_INでJPY_availableが0以下の場合は注文しないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState()

        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE))

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = BigDecimal("1000000"),
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        assertFalse(mockClient.placeOrderCalled)
        assertEquals(null, newState.realTrading.latestOrder?.orderId)
    }

    @Test
    fun `ALL_INでmaxOrderJpyを超える場合は注文しないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
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
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = BigDecimal("1000000"),
            orderSizingMode = cryptoautotrading.domain.model.OrderSizingMode.ALL_IN
        )

        assertFalse(mockClient.placeOrderCalled)
        assertEquals(null, newState.realTrading.latestOrder?.orderId)
    }

    @Test
    fun `保有中のSELL_CANDIDATEでSELLの成行注文が送信されること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.01"), BigDecimal("0.01"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01")
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("SELL", mockClient.lastPlaceOrderSide)
        assertEquals(0, BigDecimal("0.01").compareTo(mockClient.lastPlaceOrderSize))

        val latestOrder = newState.realTrading.latestOrder
        assertEquals(RealOrderSide.SELL, latestOrder?.side)
        assertEquals(RealOrderStatus.ORDERED, latestOrder?.status)
        // 注文の受付と約定は別なので、この時点で保有状態は変えない
        assertTrue(newState.isHolding)
        assertEquals(BigDecimal("0.01"), newState.holdingAmount)
    }

    @Test
    fun `取引所の残高が記録上の保有数量より多くても記録した数量しか売らないこと`() = runBlocking {
        // このアプリ以外が買ったBTCが同じ口座にある状況を想定する
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.5"), BigDecimal("0.5"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01")
        )

        service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertTrue(mockClient.placeOrderCalled)
        assertEquals(0, BigDecimal("0.01").compareTo(mockClient.lastPlaceOrderSize))
    }

    @Test
    fun `取引所の残高が記録上の保有数量より少ない場合は売らずに停止すること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.001"), BigDecimal("0.001"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01")
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertFalse(mockClient.placeOrderCalled)
        assertTrue(newState.realTrading.isStopped)
    }

    @Test
    fun `未約定注文がある場合は売り注文を出さないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.01"), BigDecimal("0.01"), BigDecimal.ONE))
        mockClient.activeOrders = listOf(ExchangeActiveOrder("active_id", "BTC", "SELL", BigDecimal("0.01"), BigDecimal.ZERO, "ORDERED"))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01")
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertFalse(mockClient.placeOrderCalled)
        assertEquals(state, newState)
    }

    @Test
    fun `isStoppedでも売り注文は実行されること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.01"), BigDecimal("0.01"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01"),
            realTrading = RealTradingState(isStopped = true, stopReason = "テスト用の停止")
        )

        service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        // 停止中でも損切りできなくなってはいけないため、売りは通す
        assertTrue(mockClient.placeOrderCalled)
        assertEquals("SELL", mockClient.lastPlaceOrderSide)
    }

    @Test
    fun `isStoppedの場合は買い注文が実行されないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(
            realTradeEnabled = true, dryRun = false,
            maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000
        )
        val state = SimulationState(
            realTrading = RealTradingState(isStopped = true, stopReason = "テスト用の停止")
        )

        service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `売り注文の約定を確認した場合、保有が解消され確定損益が加算されること`() = runBlocking {
        val decision = TradeDecision(TradeAction.HOLDING, "保有中")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            cashBalance = BigDecimal("5000"),
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01"),
            realizedProfitAndLoss = BigDecimal("100"),
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "sell_order_id",
                    symbol = "BTC",
                    side = RealOrderSide.SELL,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("11000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1100000")
                )
            )
        )

        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("sell_order_id", "EXECUTED", BigDecimal("0.01")))
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder(
                "exec_1", "sell_order_id", "BTC", "SELL",
                BigDecimal("1100000"), BigDecimal("0.01"), BigDecimal("5"), "2023-10-27T10:00:00"
            )
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertFalse(newState.isHolding)
        assertEquals(0, BigDecimal.ZERO.compareTo(newState.holdingAmount))
        assertEquals(0, BigDecimal.ZERO.compareTo(newState.buyPrice))
        // 売却代金 11000 - 手数料 5 = 10995、取得原価 10000 なので確定損益は 995。既存の100に加算される
        assertEquals(0, BigDecimal("1095").compareTo(newState.realizedProfitAndLoss))
        // 残金 5000 + 10995
        assertEquals(0, BigDecimal("15995").compareTo(newState.cashBalance))
        assertEquals(RealOrderStatus.EXECUTED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `売りが部分的にしか約定していない場合は保有が継続すること`() = runBlocking {
        val decision = TradeDecision(TradeAction.HOLDING, "保有中")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.01"),
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "partial_sell_id",
                    symbol = "BTC",
                    side = RealOrderSide.SELL,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("11000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1100000")
                )
            )
        )

        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("partial_sell_id", "EXECUTED", BigDecimal("0.004")))
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder(
                "exec_1", "partial_sell_id", "BTC", "SELL",
                BigDecimal("1100000"), BigDecimal("0.004"), BigDecimal.ZERO, "2023-10-27T10:00:00"
            )
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertTrue(newState.isHolding)
        assertEquals(0, BigDecimal("0.006").compareTo(newState.holdingAmount))
        assertEquals(0, BigDecimal("1000000").compareTo(newState.buyPrice))
    }

    @Test
    fun `買いの約定確認では従来どおり保有状態になること`() = runBlocking {
        val decision = TradeDecision(TradeAction.HOLDING, "保有中")
        val config = tradingConfig(realTradeEnabled = true, dryRun = false)
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "buy_order_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )

        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("buy_order_id", "EXECUTED", BigDecimal("0.01")))
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder(
                "exec_1", "buy_order_id", "BTC", "BUY",
                BigDecimal("1000000"), BigDecimal("0.01"), BigDecimal("5"), "2023-10-27T10:00:00"
            )
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(newState.isHolding)
        assertEquals(0, BigDecimal("0.01").compareTo(newState.holdingAmount))
        assertEquals(0, BigDecimal("1000000").compareTo(newState.buyPrice))
    }

    @Test
    fun `注文数量が取引所の刻みに丸められて送信されること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        // 1000円 ÷ 12,447,381円 = 0.00008033... なので 0.00008 に切り捨てられる
        service.executeOrderIfNeeded(decision, config, 1000, "BTC", state, BigDecimal("12447381"), BigDecimal("12447381"))

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("0.00008", mockClient.lastPlaceOrderSize?.toPlainString())
    }

    @Test
    fun `注文数量が最小注文数量に満たない場合は買い注文を見送り停止しないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        // 100円 ÷ 12,447,381円 = 0.000008... で最小注文数量 0.00001 に満たない
        val newState = service.executeOrderIfNeeded(decision, config, 100, "BTC", state, BigDecimal("12447381"), BigDecimal("12447381"))

        assertFalse(mockClient.placeOrderCalled)
        // 見送りは正常系なので停止させない
        assertFalse(newState.realTrading.isStopped)
    }

    @Test
    fun `最小注文数量に満たない端数は保有とみなされず買い注文が出せること`() = runBlocking {
        mockClient.assets = listOf(
            ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE),
            // 売却後に残ったダスト
            ExchangeAsset("BTC", BigDecimal("0.000005"), BigDecimal("0.000005"), BigDecimal.ONE)
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertTrue(mockClient.placeOrderCalled)
    }

    @Test
    fun `最小注文数量以上の残高がある場合は保有中として買い注文が出ないこと`() = runBlocking {
        mockClient.assets = listOf(
            ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE),
            ExchangeAsset("BTC", BigDecimal("0.001"), BigDecimal("0.001"), BigDecimal.ONE)
        )
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertFalse(mockClient.placeOrderCalled)
    }

    @Test
    fun `売却数量が刻みに丸められて送信されること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.5"), BigDecimal("0.5"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig()
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.00008033")
        )

        service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertTrue(mockClient.placeOrderCalled)
        assertEquals("0.00008", mockClient.lastPlaceOrderSize?.toPlainString())
    }

    @Test
    fun `保有量がダストしかない場合は売り注文を見送り停止しないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("BTC", BigDecimal("0.5"), BigDecimal("0.5"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = tradingConfig()
        val state = SimulationState(
            isHolding = true,
            buyPrice = BigDecimal("1000000"),
            holdingAmount = BigDecimal("0.000005")
        )

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1100000"), BigDecimal("1100000"))

        assertFalse(mockClient.placeOrderCalled)
        assertFalse(newState.realTrading.isStopped)
    }

    @Test
    fun `min_order_sizeが未設定の場合は注文せず停止すること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(
            realTradeEnabled = true,
            dryRun = false,
            maxOrderJpy = 20000,
            maxDailyOrderJpy = 50000,
            maxPositionJpy = 50000
        )
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(decision, config, 10000, "BTC", state, BigDecimal("1000000"), BigDecimal("1000000"))

        assertFalse(mockClient.placeOrderCalled)
        // 通常は起動時ガードで弾かれるが、万一到達したら安全側に止める
        assertTrue(newState.realTrading.isStopped)
    }

    @Test
    fun `注文数量がK線の終値ではなく取引所の最新価格で計算されること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        // K線の終値は 1,000,000 だが、取引所の最新価格は 1,004,000（0.4%の上振れ）
        service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = BigDecimal("1004000")
        )

        assertTrue(mockClient.placeOrderCalled)
        // 古いK線の終値で割ると 0.01 になり、実際の約定額が想定を超える。
        // 最新価格で割った 0.00996... を刻みに丸めた 0.00996 になること
        assertEquals("0.00996", mockClient.lastPlaceOrderSize?.toPlainString())
    }

    @Test
    fun `K線の終値と最新価格が離れすぎている場合は注文を見送ること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        // 許容スリッページ 0.5% を超える 1% の乖離
        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = BigDecimal("1010000")
        )

        assertFalse(mockClient.placeOrderCalled)
        // どちらの価格が正しいか判断できないだけなので、停止はさせない
        assertFalse(newState.realTrading.isStopped)
    }

    @Test
    fun `取引所の最新価格が取得できない場合は注文を見送ること`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = tradingConfig(maxOrderJpy = 20000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = null
        )

        assertFalse(mockClient.placeOrderCalled)
        assertFalse(newState.realTrading.isStopped)
    }

    @Test
    fun `最新価格が取得できなくても未確認注文の照合は行われること`() = runBlocking {
        val decision = TradeDecision(TradeAction.HOLDING, "保有中")
        val config = tradingConfig()
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "pending_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("pending_id", "CANCELED", BigDecimal.ZERO))

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = null
        )

        assertTrue(mockClient.getOrdersCalled)
        assertEquals(RealOrderStatus.CANCELED, newState.realTrading.latestOrder?.status)
    }

    @Test
    fun `約定価格が想定から離れすぎていた場合は新規の買いを止めること`() = runBlocking {
        val decision = TradeDecision(TradeAction.HOLDING, "保有中")
        val config = tradingConfig()
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "slipped_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("slipped_id", "EXECUTED", BigDecimal("0.01")))
        // 想定 1,000,000 に対して 1,010,000（1%）で約定した
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder(
                "exec_1", "slipped_id", "BTC", "BUY",
                BigDecimal("1010000"), BigDecimal("0.01"), BigDecimal("5"), "2023-10-27T10:00:00"
            )
        )

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1010000"),
            tickerPrice = BigDecimal("1010000")
        )

        // 約定自体は state に反映したうえで停止する
        assertTrue(newState.isHolding)
        assertTrue(newState.realTrading.isStopped)
    }

    @Test
    fun `約定価格の乖離が許容範囲内なら停止しないこと`() = runBlocking {
        val decision = TradeDecision(TradeAction.HOLDING, "保有中")
        val config = tradingConfig()
        val state = SimulationState(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "normal_id",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.ORDERED,
                    requestedAmountJpy = BigDecimal("10000"),
                    requestedSize = BigDecimal("0.01"),
                    requestedPrice = BigDecimal("1000000")
                )
            )
        )
        mockClient.mockOrdersResponse = listOf(ExchangeOrderStatus("normal_id", "EXECUTED", BigDecimal("0.01")))
        mockClient.mockExecutionsResponse = listOf(
            ExecutedOrder(
                "exec_1", "normal_id", "BTC", "BUY",
                BigDecimal("1002000"), BigDecimal("0.01"), BigDecimal("5"), "2023-10-27T10:00:00"
            )
        )

        val newState = service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1002000"),
            tickerPrice = BigDecimal("1002000")
        )

        assertTrue(newState.isHolding)
        assertFalse(newState.realTrading.isStopped)
    }

    @Test
    fun `手数料を含めた金額がmaxOrderJpyを超える場合は注文しないこと`() = runBlocking {
        mockClient.assets = listOf(ExchangeAsset("JPY", BigDecimal("20000"), BigDecimal("20000"), BigDecimal.ONE))
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        // 注文額 10000 に手数料 5 が乗って 10005 になり、上限 10000 を超える
        val config = tradingConfig(maxOrderJpy = 10000, maxDailyOrderJpy = 50000, maxPositionJpy = 50000)
        val state = SimulationState()

        service.executeOrderIfNeeded(
            decision = decision,
            config = config,
            tradeAmount = 10000,
            symbol = "BTC",
            currentState = state,
            klineClosePrice = BigDecimal("1000000"),
            tickerPrice = BigDecimal("1000000")
        )

        assertFalse(mockClient.placeOrderCalled)
    }
}

/**
 * テスト用のリアル取引設定を作る。
 * 注文数量の制約は実注文に必須なので、各テストで書かなくて済むよう既定で埋める。
 */
private fun tradingConfig(
    realTradeEnabled: Boolean = true,
    dryRun: Boolean = false,
    maxOrderJpy: Int? = null,
    maxDailyOrderJpy: Int? = null,
    maxPositionJpy: Int? = null
): RealTradingConfig = RealTradingConfig(
    realTradeEnabled = realTradeEnabled,
    dryRun = dryRun,
    maxOrderJpy = maxOrderJpy,
    maxDailyOrderJpy = maxDailyOrderJpy,
    maxPositionJpy = maxPositionJpy,
    minOrderSize = BigDecimal("0.00001"),
    sizeStep = BigDecimal("0.00001"),
    takerFeeRate = BigDecimal("0.0005"),
    maxSlippageRate = BigDecimal("0.005")
)

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
    var lastPlaceOrderSide: String? = null

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
        lastPlaceOrderSide = side
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
