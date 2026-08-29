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

        // 判定は各K線で行い、約定は次のK線の始値で行われる。
        // 判定のシナリオ（カッコ内は約定価格 = 次のK線の始値）:
        // 1回目: 買い (100) -> 数量: 10
        // 2回目: 利確売り (120) -> 損益: +200
        // 3回目: 買い (100) -> 数量: 10
        // 4回目: 損切り売り (80) -> 損益: -200
        // 5回目: 買い (100) -> 数量: 10
        // 6回目: 損切り売り (90) -> 損益: -100
        // 7回目: 買い (100) -> 数量: 10 (未売却で終了)
        // 8回目: 見送り（最後のK線の判定は約定させる足がないため実行されない）

        every { mockStrategy.judge(any(), any()) } returnsMany listOf(
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 1"),
            TradeDecision(TradeAction.SELL_CANDIDATE, "Sell 1 (Profit)"),
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 2"),
            TradeDecision(TradeAction.SELL_CANDIDATE, "Sell 2 (Loss)"),
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 3"),
            TradeDecision(TradeAction.SELL_CANDIDATE, "Sell 3 (Loss)"),
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy 4"),
            TradeDecision(TradeAction.SKIP, "Skip (最終足)")
        )

        val klines = listOf(
            Kline("1", "100", "100", "100", "100", "10"),
            Kline("2", "100", "100", "100", "100", "10"),
            Kline("3", "120", "120", "120", "120", "10"),
            Kline("4", "100", "100", "100", "100", "10"),
            Kline("5", "80", "80", "80", "80", "10"),
            Kline("6", "100", "100", "100", "100", "10"),
            Kline("7", "90", "90", "90", "90", "10"),
            Kline("8", "100", "100", "100", "100", "10")
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
            // 始値100で約定し、終値120で評価される
            Kline("2", "100", "120", "100", "120", "10")
        )
        val initialCapital = BigDecimal("10000")
        val tradeAmount = 1000 // これは無視されるはず
        val orderSizingMode = OrderSizingMode.ALL_IN

        // Act
        val result = engine.run(klines, mockStrategy, initialCapital, tradeAmount, orderSizingMode)

        // Assert
        assertEquals(2, result.steps.size)

        // 1回目のステップ: 買いシグナルは出るが、約定は次の足の始値なのでまだ保有していない
        val step1 = result.steps[0]
        assertEquals(TradeAction.BUY_CANDIDATE, step1.action)
        assertEquals(0, BigDecimal("10000").compareTo(step1.cashBalance))
        assertEquals(0, BigDecimal.ZERO.compareTo(step1.holdingAmount))

        // 2回目のステップ: 始値100で約定済み
        val step2 = result.steps[1]
        assertEquals(TradeAction.SKIP, step2.action)
        assertEquals(0, BigDecimal.ZERO.compareTo(step2.cashBalance)) // 残高は0になる
        assertEquals(0, BigDecimal("100").compareTo(step2.holdingAmount)) // 10000 / 100 = 100
        assertEquals(0, BigDecimal("100").compareTo(step2.buyPrice))
        // 資産評価額は 100 * 120 = 12000 になる
        assertEquals(0, BigDecimal("12000").compareTo(step2.totalAssetValue))
    }

    @Test
    fun `判定に使ったK線の終値ではなく次のK線の始値で約定すること`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        every { mockStrategy.judge(any(), any()) } returnsMany listOf(
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy"),
            TradeDecision(TradeAction.SKIP, "Skip")
        )

        val klines = listOf(
            // 判定に使う足。終値は100
            Kline("1", "100", "100", "100", "100", "10"),
            // 約定させる足。始値は200
            Kline("2", "200", "200", "200", "200", "10")
        )

        // Act
        val result = engine.run(klines, mockStrategy, BigDecimal("10000"), 1000)

        // Assert
        // 終値100で約定していたら買値は100・数量は10になる。次足の始値200で約定するのが正しい
        val step2 = result.steps[1]
        assertEquals(0, BigDecimal("200").compareTo(step2.buyPrice))
        assertEquals(0, BigDecimal("5").compareTo(step2.holdingAmount))
    }

    @Test
    fun `最後のK線で出たシグナルは約定しないこと`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        every { mockStrategy.judge(any(), any()) } returns TradeDecision(TradeAction.BUY_CANDIDATE, "Buy")

        val klines = listOf(
            Kline("1", "100", "100", "100", "100", "10"),
            Kline("2", "100", "100", "100", "100", "10")
        )

        // Act
        val result = engine.run(klines, mockStrategy, BigDecimal("10000"), 1000)

        // Assert
        // 2本とも買いシグナルだが、約定するのは1本目のシグナルだけ
        assertEquals(1, result.summary.buyCount)
    }

    @Test
    fun `手数料とスリッページが約定価格に反映されること`() {
        // Arrange
        val engine = BacktestEngine()
        val mockStrategy = mockk<TradingStrategy>()

        every { mockStrategy.judge(any(), any()) } returnsMany listOf(
            TradeDecision(TradeAction.BUY_CANDIDATE, "Buy"),
            TradeDecision(TradeAction.SKIP, "Skip")
        )

        val klines = listOf(
            Kline("1", "100", "100", "100", "100", "10"),
            Kline("2", "100", "100", "100", "100", "10")
        )
        val costConfig = BacktestCostConfig(
            feeRate = BigDecimal("0.01"),
            slippageRate = BigDecimal("0.01")
        )

        // Act
        val result = engine.run(
            klines = klines,
            strategy = mockStrategy,
            initialCapital = BigDecimal("10000"),
            tradeAmount = 1000,
            costConfig = costConfig
        )

        // Assert
        // 買いは不利な方向に寄る: 100 * 1.01 * 1.01 = 102.01
        assertEquals(0, BigDecimal("102.01").compareTo(result.steps[1].buyPrice))
        assertEquals(0, BigDecimal("0.01").compareTo(result.summary.feeRate))
        assertEquals(0, BigDecimal("0.01").compareTo(result.summary.slippageRate))
    }
}
