package cryptoautotrading.domain.realtrading

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.realtrading.RealOrderSide
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.realtrading.RealOrderState
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.domain.model.realtrading.RealTradingState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RealTradingSafetyCheckerTest {

    private lateinit var checker: RealTradingSafetyChecker
    private lateinit var defaultConfig: RealTradingConfig
    private lateinit var defaultState: SimulationState
    private val currentPrice = BigDecimal("10000000") // 1,000,0000 JPY
    private val TODAY = "2026-08-29"

    @BeforeEach
    fun setUp() {
        checker = RealTradingSafetyChecker()
        defaultConfig = RealTradingConfig(
            dryRun = false,
            realTradeEnabled = true,
            stopOnUnconfirmedOrder = true,
            maxOrderJpy = 10000,
            maxDailyOrderJpy = 50000,
            maxPositionJpy = 100000
        )
        defaultState = SimulationState()
    }

    @Test
    fun `すべての安全条件を満たす場合はpassedがtrueになること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertTrue(result.passed)
        assertEquals(null, result.reason)
    }

    @Test
    fun `realTrading_isStoppedがtrueの場合は注文不可になること`() {
        val state = defaultState.copy(
            realTrading = RealTradingState(isStopped = true)
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000,
            state = state,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("realTrading.isStopped=true", result.reason)
    }

    @Test
    fun `既に保有中(state)の場合は注文不可になること`() {
        val state = defaultState.copy(isHolding = true)

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000,
            state = state,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("既に保有中", result.reason)
    }

    @Test
    fun `既に保有中(assets)の場合は注文不可になること`() {
        val assets = listOf(ExchangeAsset("BTC", BigDecimal("0.001"), BigDecimal("0.001"), BigDecimal("10000000")))

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000,
            state = defaultState,
            currentHoldingAssets = assets,
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("既に保有中", result.reason)
    }

    @Test
    fun `未確認注文(state)がある場合は注文不可になること`() {
        val state = defaultState.copy(
            realTrading = RealTradingState(
                latestOrder = RealOrderState(
                    orderId = "123",
                    symbol = "BTC",
                    side = RealOrderSide.BUY,
                    status = RealOrderStatus.WAITING,
                    requestedAmountJpy = BigDecimal("1000"),
                    requestedSize = BigDecimal("0.0001")
                )
            )
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000,
            state = state,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("未確認または受付中の注文が存在", result.reason)
    }

    @Test
    fun `未確認注文(activeOrders)がある場合は注文不可になること`() {
        val activeOrders = listOf(
            ExchangeActiveOrder("123", "BTC", "BUY", BigDecimal("0.0001"), BigDecimal.ZERO, "WAITING")
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = activeOrders,
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("未確認または受付中の注文が存在", result.reason)
    }

    @Test
    fun `tradeAmountがmaxOrderJpyを超える場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 10001,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("trade_amountがmax_order_jpy超過", result.reason)
    }

    @Test
    fun `maxOrderJpyが未設定の場合は注文不可になること`() {
        val config = defaultConfig.copy(maxOrderJpy = null)

        val result = checker.checkPreOrderSafety(
            config = config,
            tradeAmount = 5000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("max_order_jpyが未設定", result.reason)
    }

    @Test
    fun `dailyOrderedJpyとtradeAmountの合計がmaxDailyOrderJpyを超える場合は注文不可になること`() {
        val state = defaultState.copy(
            realTrading = RealTradingState(dailyOrderedDate = TODAY, dailyOrderedJpy = BigDecimal("46000"))
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000, // 46000 + 5000 = 51000 > 50000
            state = state,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("1日の注文限度額超過", result.reason)
    }

    @Test
    fun `maxDailyOrderJpyが未設定の場合は注文不可になること`() {
        val config = defaultConfig.copy(maxDailyOrderJpy = null)

        val result = checker.checkPreOrderSafety(
            config = config,
            tradeAmount = 5000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("max_daily_order_jpyが未設定", result.reason)
    }

    @Test
    fun `現在の保有金額とtradeAmountの合計がmaxPositionJpyを超える場合は注文不可になること`() {
        val state = defaultState.copy(
            holdingAmount = BigDecimal("0.0096") // 0.0096 * 10,000,000 = 96,000 JPY
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 5000, // 96000 + 5000 = 101000 > 100000
            state = state,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("保有金額と注文予定額の合計がmax_position_jpy超過", result.reason)
    }

    @Test
    fun `maxPositionJpyが未設定の場合は注文不可になること`() {
        val config = defaultConfig.copy(maxPositionJpy = null)

        val result = checker.checkPreOrderSafety(
            config = config,
            tradeAmount = 5000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("max_position_jpyが未設定", result.reason)
    }

    @Test
    fun `売り 条件を満たす場合は注文可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.01"),
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("0.01"),
            state = SimulationState(isHolding = true, holdingAmount = BigDecimal("0.01")),
            activeOrders = emptyList()
        )

        assertTrue(result.passed)
    }

    @Test
    fun `売り 売却数量が0以下の場合は注文不可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal.ZERO,
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("0.01"),
            state = SimulationState(isHolding = true, holdingAmount = BigDecimal("0.01")),
            activeOrders = emptyList()
        )

        assertFalse(result.passed)
    }

    @Test
    fun `売り 記録上の保有数量を超える売却は注文不可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.02"),
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("0.5"),
            state = SimulationState(isHolding = true, holdingAmount = BigDecimal("0.01")),
            activeOrders = emptyList()
        )

        assertFalse(result.passed)
    }

    @Test
    fun `売り 取引所の売却可能残高を超える売却は注文不可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.01"),
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("0.005"),
            state = SimulationState(isHolding = true, holdingAmount = BigDecimal("0.01")),
            activeOrders = emptyList()
        )

        assertFalse(result.passed)
    }

    @Test
    fun `売り 未約定注文がある場合は注文不可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.01"),
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("0.01"),
            state = SimulationState(isHolding = true, holdingAmount = BigDecimal("0.01")),
            activeOrders = listOf(
                ExchangeActiveOrder("active_id", "BTC", "SELL", BigDecimal("0.01"), BigDecimal.ZERO, "ORDERED")
            )
        )

        assertFalse(result.passed)
    }

    @Test
    fun `売り isStoppedでも注文可のままであること`() {
        // 停止中に売りまで止めると、ポジションを抱えたまま損切りできなくなる
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.01"),
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("0.01"),
            state = SimulationState(
                isHolding = true,
                holdingAmount = BigDecimal("0.01"),
                realTrading = RealTradingState(isStopped = true, stopReason = "テスト用の停止")
            ),
            activeOrders = emptyList()
        )

        assertTrue(result.passed)
    }

    @Test
    fun `買い 注文予定金額が0以下の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 0,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("注文予定金額 (0) が0以下", result.reason)
    }

    @Test
    fun `買い 注文予定金額が負の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = -1000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
    }

    @Test
    fun `買い 注文に使う価格が0以下の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 1000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = BigDecimal.ZERO,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("注文に使う価格 (0) が0以下", result.reason)
    }

    @Test
    fun `買い 保有数量が負の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 1000,
            state = defaultState.copy(holdingAmount = BigDecimal("-0.01")),
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
    }

    @Test
    fun `買い 1日の累計注文額が負の場合は注文不可になること`() {
        val brokenState = defaultState.copy(
            realTrading = defaultState.realTrading.copy(dailyOrderedDate = TODAY, dailyOrderedJpy = BigDecimal("-1"))
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 1000,
            state = brokenState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
    }

    @Test
    fun `買い max_order_jpyが0以下の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig.copy(maxOrderJpy = 0),
            tradeAmount = 1000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("max_order_jpy (0) が0以下", result.reason)
    }

    @Test
    fun `買い max_daily_order_jpyが0以下の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig.copy(maxDailyOrderJpy = -1),
            tradeAmount = 1000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
    }

    @Test
    fun `買い max_position_jpyが0以下の場合は注文不可になること`() {
        val result = checker.checkPreOrderSafety(
            config = defaultConfig.copy(maxPositionJpy = 0),
            tradeAmount = 1000,
            state = defaultState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
    }

    @Test
    fun `買い 入力値の検証は停止フラグより先に行われること`() {
        // 停止中かどうかに関係なく、不正値はそれ自体の理由で拒否されることを固定する
        val stoppedState = defaultState.copy(
            realTrading = defaultState.realTrading.copy(isStopped = true)
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 0,
            state = stoppedState,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertFalse(result.passed)
        assertEquals("注文予定金額 (0) が0以下", result.reason)
    }

    @Test
    fun `売り 記録上の保有数量が0以下の場合は注文不可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.01"),
            recordedHoldingSize = BigDecimal.ZERO,
            exchangeAvailableSize = BigDecimal("0.01"),
            state = SimulationState(),
            activeOrders = emptyList()
        )

        assertFalse(result.passed)
    }

    @Test
    fun `売り 取引所の売却可能残高が負の場合は注文不可になること`() {
        val result = checker.checkPreSellOrderSafety(
            sellSize = BigDecimal("0.01"),
            recordedHoldingSize = BigDecimal("0.01"),
            exchangeAvailableSize = BigDecimal("-0.01"),
            state = SimulationState(isHolding = true, holdingAmount = BigDecimal("0.01")),
            activeOrders = emptyList()
        )

        assertFalse(result.passed)
    }

    @Test
    fun `日付が変わっていれば前日の累計注文額は判定に使われないこと`() {
        // 前日に上限近くまで注文していても、日付が変われば当日分は0から数え直す。
        // 日付を見ないと、上限に近づいた翌日以降は1件も注文できなくなる。
        val stateWithYesterdayTotal = defaultState.copy(
            realTrading = defaultState.realTrading.copy(
                dailyOrderedDate = "2026-08-28",
                dailyOrderedJpy = BigDecimal("46000")
            )
        )

        val result = checker.checkPreOrderSafety(
            config = defaultConfig,
            tradeAmount = 1000,
            state = stateWithYesterdayTotal,
            currentHoldingAssets = emptyList(),
            activeOrders = emptyList(),
            currentPrice = currentPrice,
            today = TODAY
        )

        assertTrue(result.passed)
    }
}
