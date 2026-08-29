package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.OrderSizingMode
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.strategy.TradingStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * バックテストを実行するエンジン
 */
class BacktestEngine {

    private val logger = KotlinLogging.logger {}
    private val simulationService = SimulationService()

    /**
     * 過去K線データを使って指定された戦略をバックテストする。
     *
     * 判定に使ったK線の終値でそのまま約定させると、終値を見てから同じ終値で売買できる
     * ことになり成績が過大評価される。そのため、K線 N の確定後に出したシグナルは
     * **K線 N+1 の始値**で約定させる。最後のK線で出たシグナルは約定させる足がないため実行しない。
     *
     * @param klines 過去K線データのリスト (openTime昇順でソートされている前提)
     * @param strategy 使用する売買戦略
     * @param initialCapital 初期資金
     * @param tradeAmount 1回の取引額
     * @param orderSizingMode 注文数量モード (デフォルト: FIXED_AMOUNT)
     * @param costConfig 手数料とスリッページの設定 (デフォルト: いずれも0)
     * @return バックテスト結果
     */
    fun run(
        klines: List<Kline>,
        strategy: TradingStrategy,
        initialCapital: BigDecimal,
        tradeAmount: Int,
        orderSizingMode: OrderSizingMode = OrderSizingMode.FIXED_AMOUNT,
        costConfig: BacktestCostConfig = BacktestCostConfig()
    ): BacktestResult {
        logger.info {
            "バックテストを開始します。データ件数: ${klines.size}, 初期資金: $initialCapital, " +
                "注文数量モード: $orderSizingMode, 手数料率: ${costConfig.feeRate}, スリッページ率: ${costConfig.slippageRate}"
        }

        var currentState = SimulationState(cashBalance = initialCapital)

        val steps = mutableListOf<BacktestStepResult>()
        var buyCount = 0
        var sellCount = 0
        var maxTotalAssetValue = initialCapital
        var maxDrawdown = BigDecimal.ZERO

        var takeProfitCount = 0
        var stopLossCount = 0
        var totalProfit = BigDecimal.ZERO
        var totalLoss = BigDecimal.ZERO
        var maxProfit = BigDecimal.ZERO
        var maxLoss = BigDecimal.ZERO
        var maxConsecutiveLossCount = 0
        var currentConsecutiveLossCount = 0

        val processedKlines = mutableListOf<Kline>()

        // 直前のK線で出たシグナル。次のK線の始値で約定させる
        var pendingDecision: TradeDecision? = null

        for (kline in klines) {
            val currentPrice = BigDecimal(kline.close)
            val previousIsHolding = currentState.isHolding
            val previousRealizedProfitAndLoss = currentState.realizedProfitAndLoss

            // 直前のK線で出たシグナルを、このK線の始値で約定させる
            val executedDecision = pendingDecision
            if (executedDecision != null) {
                val executionPrice = resolveExecutionPrice(
                    action = executedDecision.action,
                    openPrice = BigDecimal(kline.open),
                    costConfig = costConfig
                )
                currentState = simulationService.updateState(
                    currentState = currentState,
                    decision = executedDecision,
                    currentPrice = executionPrice,
                    tradeAmount = tradeAmount,
                    eventTime = kline.openTime,
                    orderSizingMode = orderSizingMode
                )
            }

            processedKlines.add(kline)

            // 戦略による判定。結果は次のK線の始値で約定させる
            val decision = strategy.judge(processedKlines, currentState)
            pendingDecision = decision

            // 買い・売りのカウント
            if (!previousIsHolding && currentState.isHolding) {
                buyCount++
            } else if (previousIsHolding && !currentState.isHolding) {
                sellCount++

                // 売却時の損益計算
                val tradeProfit = currentState.realizedProfitAndLoss - previousRealizedProfitAndLoss

                if (tradeProfit > BigDecimal.ZERO) {
                    takeProfitCount++
                    totalProfit += tradeProfit
                    if (tradeProfit > maxProfit) {
                        maxProfit = tradeProfit
                    }
                    currentConsecutiveLossCount = 0
                } else if (tradeProfit < BigDecimal.ZERO) {
                    stopLossCount++
                    totalLoss += tradeProfit
                    if (tradeProfit < maxLoss) {
                        maxLoss = tradeProfit
                    }
                    currentConsecutiveLossCount++
                    if (currentConsecutiveLossCount > maxConsecutiveLossCount) {
                        maxConsecutiveLossCount = currentConsecutiveLossCount
                    }
                } else {
                    // 損益ゼロの場合（利確でも損切りでもない場合は、便宜上連続損切りはリセットする仕様とする。要件によっては変更余地あり）
                    currentConsecutiveLossCount = 0
                }
            }

            // 資産の計算
            val estimatedHoldingValue = currentState.holdingAmount * currentPrice
            val totalAssetValue = currentState.cashBalance + estimatedHoldingValue

            // 最大ドローダウンの計算
            if (totalAssetValue > maxTotalAssetValue) {
                maxTotalAssetValue = totalAssetValue
            } else {
                val drawdown = (maxTotalAssetValue - totalAssetValue).divide(maxTotalAssetValue, 8, RoundingMode.HALF_UP)
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown
                }
            }

            steps.add(
                BacktestStepResult(
                    openTime = kline.openTime,
                    close = kline.close,
                    action = decision.action,
                    reason = decision.reason,
                    cashBalance = currentState.cashBalance,
                    holdingAmount = currentState.holdingAmount,
                    buyPrice = currentState.buyPrice,
                    realizedProfitAndLoss = currentState.realizedProfitAndLoss,
                    estimatedHoldingValue = estimatedHoldingValue,
                    totalAssetValue = totalAssetValue
                )
            )
        }

        val finalAssetValue = if (steps.isNotEmpty()) steps.last().totalAssetValue else initialCapital
        val realizedProfitAndLoss = currentState.realizedProfitAndLoss

        // totalReturnRate = (finalAssetValue - initialCapital) / initialCapital
        val totalReturnRate = if (initialCapital > BigDecimal.ZERO) {
            (finalAssetValue - initialCapital).divide(initialCapital, 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val winRate = if (sellCount > 0) {
            BigDecimal(takeProfitCount).divide(BigDecimal(sellCount), 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val averageProfit = if (takeProfitCount > 0) {
            totalProfit.divide(BigDecimal(takeProfitCount), 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val averageLoss = if (stopLossCount > 0) {
            totalLoss.divide(BigDecimal(stopLossCount), 8, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val summary = BacktestSummary(
            strategyName = strategy::class.simpleName ?: "UnknownStrategy",
            initialCapital = initialCapital,
            finalAssetValue = finalAssetValue,
            realizedProfitAndLoss = realizedProfitAndLoss,
            totalReturnRate = totalReturnRate,
            tradeCount = buyCount + sellCount,
            buyCount = buyCount,
            sellCount = sellCount,
            maxDrawdown = maxDrawdown,
            takeProfitCount = takeProfitCount,
            stopLossCount = stopLossCount,
            winRate = winRate,
            averageProfit = averageProfit,
            averageLoss = averageLoss,
            maxProfit = maxProfit,
            maxLoss = maxLoss,
            maxConsecutiveLossCount = maxConsecutiveLossCount,
            hasOpenPosition = currentState.isHolding,
            feeRate = costConfig.feeRate,
            slippageRate = costConfig.slippageRate
        )

        logger.info { "バックテストが完了しました。総資産: $finalAssetValue, 利益率: $totalReturnRate, ドローダウン: $maxDrawdown" }

        return BacktestResult(summary = summary, steps = steps)
    }

    /**
     * 約定価格を決定する。
     *
     * 手数料とスリッページを価格に織り込む。買いは不利な方向（高く）に、
     * 売りは不利な方向（安く）に寄せることで、実際の取引に近づける。
     * 買い・売り以外のアクションでは始値をそのまま返す。
     *
     * @param action 約定させる売買アクション
     * @param openPrice 約定させるK線の始値
     * @param costConfig 手数料とスリッページの設定
     * @return コストを織り込んだ約定価格
     */
    private fun resolveExecutionPrice(
        action: TradeAction,
        openPrice: BigDecimal,
        costConfig: BacktestCostConfig
    ): BigDecimal {
        return when (action) {
            TradeAction.BUY_CANDIDATE ->
                openPrice * (BigDecimal.ONE + costConfig.slippageRate) * (BigDecimal.ONE + costConfig.feeRate)
            TradeAction.SELL_CANDIDATE ->
                openPrice * (BigDecimal.ONE - costConfig.slippageRate) * (BigDecimal.ONE - costConfig.feeRate)
            TradeAction.SKIP, TradeAction.HOLDING -> openPrice
        }
    }
}
