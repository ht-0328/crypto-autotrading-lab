package cryptoautotrading.domain.backtest

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
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
     * @param klines 過去K線データのリスト (openTime昇順でソートされている前提)
     * @param strategy 使用する売買戦略
     * @param initialCapital 初期資金
     * @param tradeAmount 1回の取引額
     * @return バックテスト結果
     */
    fun run(
        klines: List<Kline>,
        strategy: TradingStrategy,
        initialCapital: BigDecimal,
        tradeAmount: Int
    ): BacktestResult {
        logger.info { "バックテストを開始します。データ件数: ${klines.size}, 初期資金: $initialCapital" }

        var currentState = SimulationState(cashBalance = initialCapital)

        val steps = mutableListOf<BacktestStepResult>()
        var buyCount = 0
        var sellCount = 0
        var maxTotalAssetValue = initialCapital
        var maxDrawdown = BigDecimal.ZERO

        val processedKlines = mutableListOf<Kline>()

        for (kline in klines) {
            processedKlines.add(kline)

            val currentPrice = BigDecimal(kline.close)

            // 戦略による判定
            val decision = strategy.judge(processedKlines, currentState)

            val previousIsHolding = currentState.isHolding

            // 状態の更新
            currentState = simulationService.updateState(
                currentState = currentState,
                decision = decision,
                currentPrice = currentPrice,
                tradeAmount = tradeAmount
            )

            // 買い・売りのカウント
            if (!previousIsHolding && currentState.isHolding) {
                buyCount++
            } else if (previousIsHolding && !currentState.isHolding) {
                sellCount++
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

        val summary = BacktestSummary(
            strategyName = strategy::class.simpleName ?: "UnknownStrategy",
            initialCapital = initialCapital,
            finalAssetValue = finalAssetValue,
            realizedProfitAndLoss = realizedProfitAndLoss,
            totalReturnRate = totalReturnRate,
            tradeCount = buyCount + sellCount,
            buyCount = buyCount,
            sellCount = sellCount,
            maxDrawdown = maxDrawdown
        )

        logger.info { "バックテストが完了しました。総資産: $finalAssetValue, 利益率: $totalReturnRate, ドローダウン: $maxDrawdown" }

        return BacktestResult(summary = summary, steps = steps)
    }
}
