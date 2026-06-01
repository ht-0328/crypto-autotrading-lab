package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.OrderSizingMode
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.strategy.TradingStrategy
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BacktestEngineTest {

    @Test
    fun `初期資金10000で取引を行わない場合、終了時の総資産は10000のままであること`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        // 常に「見送り」を返す戦略
        every { mockStrategy.judge(any(), any()) } returns TradeDecision(TradeAction.SKIP, "テスト用見送り")

        val klines = listOf(
            Kline("202605010000", "100", "110", "90", "105", "10"),
            Kline("202605010005", "105", "115", "95", "110", "15")
        )
        val initialCapital = BigDecimal("10000")
        val tradeAmount = 1000

        // Act
        val result = engine.run(klines, mockStrategy, initialCapital, tradeAmount)

        // Assert
        val summary = result.summary
        assertEquals(0, BigDecimal("10000").compareTo(summary.initialCapital))
        assertEquals(0, BigDecimal("10000").compareTo(summary.finalAssetValue))
        assertEquals(0, summary.buyCount)
        assertEquals(0, summary.sellCount)
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.maxDrawdown))

        assertEquals(2, result.steps.size)
        assertEquals(TradeAction.SKIP, result.steps[0].action)
        assertEquals(0, BigDecimal("10000").compareTo(result.steps[0].cashBalance))
    }

    @Test
    fun `取引が行われ、利確と損切りが正しく集計されること`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        // 判定のシナリオ:
        // 1回目: 買い (価格100) -> 数量: 10
        // 2回目: 利確売り (価格120) -> 損益: +200
        // 3回目: 買い (価格100) -> 数量: 10
        // 4回目: 損切り売り (価格80) -> 損益: -200
        // 5回目: 買い (価格100) -> 数量: 10
        // 6回目: 損切り売り (価格90) -> 損益: -100
        // 7回目: 買い (価格100) -> 数量: 10 (未売却で終了)

        every { mockStrategy.judge(any(), any()) } returnsMany listOf(
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 1"),
            TradeDecision(TradeAction.SELL_CANDIDATE, "Sell 1 (Profit)"),
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 2"),
            TradeDecision(TradeAction.SELL_CANDIDATE, "Sell 2 (Loss)"),
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 3"),
            TradeDecision(TradeAction.SELL_CANDIDATE, "Sell 3 (Loss)"),
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 4")
        )

        val klines = listOf(
            Kline("1", "100", "100", "100", "100", "10"),
            Kline("2", "120", "120", "120", "120", "10"),
            Kline("3", "100", "100", "100", "100", "10"),
            Kline("4", "80", "80", "80", "80", "10"),
            Kline("5", "100", "100", "100", "100", "10"),
            Kline("6", "90", "90", "90", "90", "10"),
            Kline("7", "100", "100", "100", "100", "10")
        )
        val initialCapital = BigDecimal("10000")
        val tradeAmount = 1000

        // Act
        val result = engine.run(klines, mockStrategy, initialCapital, tradeAmount)

        // Assert
        val summary = result.summary

        // 基本的なカウントの確認
        assertEquals(4, summary.buyCount)
        assertEquals(3, summary.sellCount)
        assertEquals(7, summary.tradeCount)

        // 拡張指標の確認
        assertEquals(1, summary.takeProfitCount)
        assertEquals(2, summary.stopLossCount)

        // 勝率は 1/3 = 0.33333333
        assertEquals(0, BigDecimal("0.33333333").compareTo(summary.winRate))

        // 利確は+200が1回
        assertEquals(0, BigDecimal("200").compareTo(summary.averageProfit))
        assertEquals(0, BigDecimal("200").compareTo(summary.maxProfit))

        // 損切りは-200と-100の2回。平均は-150、最大損失は-200(最もマイナスが大きいもの)
        assertEquals(0, BigDecimal("-150").compareTo(summary.averageLoss))
        assertEquals(0, BigDecimal("-200").compareTo(summary.maxLoss))

        // 連続損切り回数は2回 (Sell 2, Sell 3 が連続)
        assertEquals(2, summary.maxConsecutiveLossCount)

        // 最後はBuy 4で終わっているので、未売却のポジションがある
        assertEquals(true, summary.hasOpenPosition)
    }

    @Test
    fun `OrderSizingModeがALL_INの場合、残高全てを使って購入が行われること`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        // 1回目: 買い (価格100) -> ALL_INなので10000円分すべて買うはず
        // 2回目: ホールド
        every { mockStrategy.judge(any(), any()) } returnsMany listOf(
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 1"),
            TradeDecision(TradeAction.SKIP, "Skip")
        )

        val klines = listOf(
            Kline("1", "100", "100", "100", "100", "10"),
            Kline("2", "120", "120", "120", "120", "10")
        )
        val initialCapital = BigDecimal("10000")
        val tradeAmount = 1000 // これは無視されるはず
        val orderSizingMode = OrderSizingMode.ALL_IN

        // Act
        val result = engine.run(klines, mockStrategy, initialCapital, tradeAmount, orderSizingMode)

        // Assert
        assertEquals(2, result.steps.size)

        // 1回目のステップ（購入）
        val step1 = result.steps[0]
        assertEquals(TradeAction.BUY_CANDIDATE, step1.action)
        assertEquals(0, BigDecimal.ZERO.compareTo(step1.cashBalance)) // 残高は0になる
        assertEquals(0, BigDecimal("100").compareTo(step1.holdingAmount)) // 10000 / 100 = 100
        assertEquals(0, BigDecimal("100").compareTo(step1.buyPrice))

        // 2回目のステップ（スキップ）
        val step2 = result.steps[1]
        assertEquals(TradeAction.SKIP, step2.action)
        assertEquals(0, BigDecimal.ZERO.compareTo(step2.cashBalance)) // 残高は0のまま
        assertEquals(0, BigDecimal("100").compareTo(step2.holdingAmount)) // 数量は100のまま
        // 資産評価額は 100 * 120 = 12000 になる
        assertEquals(0, BigDecimal("12000").compareTo(step2.totalAssetValue))
    }
}
