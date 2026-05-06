package cryptoautotrading.domain.simulation

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * シミュレーションの状態を更新するサービス
 */
class SimulationService {

    private val logger = KotlinLogging.logger {}

    /**
     * 売買判定結果に基づいてシミュレーション状態を更新する
     *
     * @param currentState 現在のシミュレーション状態
     * @param decision 売買判定結果
     * @param currentPrice 現在の価格
     * @param tradeAmount 1回の取引額
     * @param eventTime イベント発生時刻（K線の時刻など）。指定がない場合は現在時刻が使用される。
     * @return 更新後のシミュレーション状態
     */
    fun updateState(
        currentState: SimulationState,
        decision: TradeDecision,
        currentPrice: BigDecimal,
        tradeAmount: Int,
        eventTime: String = ""
    ): SimulationState {
        logger.debug { "シミュレーション状態の更新処理を開始します" }
        logger.debug { "更新前状態: $currentState, 判定結果: ${decision.action}, 現在価格: $currentPrice, 取引額: $tradeAmount, イベント時刻: $eventTime" }

        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val timeToRecord = if (eventTime.isNotBlank()) eventTime else nowStr

        val nextState = when (decision.action) {
            TradeAction.BUY_CANDIDATE -> {
                val tradeAmountBd = BigDecimal(tradeAmount)
                if (!currentState.isHolding && currentState.cashBalance >= tradeAmountBd) {
                    // 購入する
                    val amount = tradeAmountBd.divide(currentPrice, 8, RoundingMode.DOWN)
                    val actualBuyAmount = amount * currentPrice
                    SimulationState(
                        cashBalance = currentState.cashBalance - actualBuyAmount,
                        isHolding = true,
                        buyPrice = currentPrice,
                        holdingAmount = amount,
                        realizedProfitAndLoss = currentState.realizedProfitAndLoss,
                        lastUpdatedAt = nowStr,
                        lastStopLossTime = currentState.lastStopLossTime
                    )
                } else {
                    // すでに保有している、または残金不足の場合は状態を維持
                    currentState.copy(lastUpdatedAt = nowStr)
                }
            }
            TradeAction.SELL_CANDIDATE -> {
                if (currentState.isHolding && currentState.holdingAmount > BigDecimal.ZERO && currentState.buyPrice > BigDecimal.ZERO) {
                    // 売却する
                    val sellAmount = currentState.holdingAmount * currentPrice
                    val buyAmount = currentState.holdingAmount * currentState.buyPrice
                    val profitAndLoss = sellAmount - buyAmount

                    val isStopLoss = profitAndLoss < BigDecimal.ZERO
                    val newStopLossTime = if (isStopLoss) timeToRecord else currentState.lastStopLossTime

                    SimulationState(
                        cashBalance = currentState.cashBalance + sellAmount,
                        isHolding = false,
                        buyPrice = BigDecimal.ZERO,
                        holdingAmount = BigDecimal.ZERO,
                        realizedProfitAndLoss = currentState.realizedProfitAndLoss + profitAndLoss,
                        lastUpdatedAt = nowStr,
                        lastStopLossTime = newStopLossTime
                    )
                } else {
                    // 保有していない、または売却に必要なデータがない場合は状態を維持
                    currentState.copy(lastUpdatedAt = nowStr)
                }
            }
            TradeAction.SKIP, TradeAction.HOLDING -> {
                // 状態を維持するが更新日時は更新する
                currentState.copy(lastUpdatedAt = nowStr)
            }
        }

        logger.debug { "シミュレーション状態の更新が完了しました" }
        logger.debug { "更新後状態: $nextState" }

        return nextState
    }
}
