package cryptoautotrading.domain.realtrading

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.realtrading.RealOrderSide
import cryptoautotrading.domain.model.realtrading.RealOrderState
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.domain.realtrading.RealTradingClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * リアル取引の注文処理を担当するサービス
 *
 * @property exchangeClient リアル取引の取引所操作を行うクライアント
 * @property safetyChecker 実注文前安全チェックを行うサービス
 */
class RealTradingService(
    private val exchangeClient: RealTradingClient? = null,
    private val safetyChecker: RealTradingSafetyChecker = RealTradingSafetyChecker()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 判定結果と設定に基づいてリアル注文を実行するかどうかを判断し、処理を行う。
     *
     * BUY_CANDIDATE の場合、安全チェックを通過した場合のみ GMO Private API の placeOrder を呼び、
     * 返ってきた orderId を state の realTrading.latestOrder に保存する。
     *
     * @param decision 戦略判定によって下された売買判定結果
     * @param config リアル取引に関する設定
     * @param tradeAmount 1回あたりの注文予定金額(JPY)
     * @param symbol 銘柄名
     * @param currentState 現在のシミュレーション(およびリアル取引)の状態
     * @param currentPrice 現在の市場価格
     * @return 処理後のシミュレーション状態
     */
    suspend fun executeOrderIfNeeded(
        decision: TradeDecision,
        config: RealTradingConfig,
        tradeAmount: Int,
        symbol: String,
        currentState: SimulationState,
        currentPrice: BigDecimal
    ): SimulationState {
        if (shouldSkipRealTrading(config)) {
            return currentState
        }

        if (handleMissingExchangeClient(decision.action)) {
            return currentState
        }

        if (hasUnconfirmedOrder(currentState)) {
            return checkLatestOrderStatus(currentState)
        }

        return when (decision.action) {
            TradeAction.BUY_CANDIDATE -> executeBuyCandidateOrder(
                config = config,
                tradeAmount = tradeAmount,
                symbol = symbol,
                currentState = currentState,
                currentPrice = currentPrice
            )
            TradeAction.SELL_CANDIDATE -> {
                logger.info { "リアル取引: SELL_CANDIDATE を検知しましたが、現行フェーズでは売り注文は実行しません。" }
                currentState
            }
            else -> {
                logger.debug { "Trade action is not BUY_CANDIDATE (${decision.action}). No real trade action taken." }
                currentState
            }
        }
    }

    /**
     * リアル取引をスキップすべきかどうかを判定する
     */
    private fun shouldSkipRealTrading(config: RealTradingConfig): Boolean {
        if (config.dryRun || !config.realTradeEnabled) {
            logger.debug { "Real trading is disabled or in dry-run mode. Skipping real trade execution." }
            return true
        }
        return false
    }

    /**
     * exchangeClient が未設定の場合の処理を行う
     *
     * @return スキップすべき場合は true
     */
    private fun handleMissingExchangeClient(action: TradeAction): Boolean {
        if (exchangeClient == null) {
            if (action == TradeAction.BUY_CANDIDATE) {
                logger.warn { "リアル取引: exchangeClient が設定されていないため、実注文処理をスキップします。" }
            }
            return true
        }
        return false
    }

    /**
     * 状態に未確認の注文が存在するかどうかを判定する
     */
    private fun hasUnconfirmedOrder(currentState: SimulationState): Boolean {
        val latestOrder = currentState.realTrading.latestOrder ?: return false
        return latestOrder.status == RealOrderStatus.ORDERED ||
               latestOrder.status == RealOrderStatus.WAITING ||
               latestOrder.status == RealOrderStatus.UNCONFIRMED
    }

    /**
     * 未確認の最新注文の状態を確認し、シミュレーション状態に反映する
     */
    private suspend fun checkLatestOrderStatus(currentState: SimulationState): SimulationState {
        val latestOrder = currentState.realTrading.latestOrder ?: return currentState
        logger.info { "リアル取引: 未確認注文 (orderId: ${latestOrder.orderId}) が存在します。状態を確認します。" }

        try {
            val client = exchangeClient ?: return currentState
            val orders = client.getOrders(latestOrder.orderId)
            val targetOrder = orders.find { it.orderId == latestOrder.orderId }

            if (targetOrder != null) {
                val mappedStatus = mapExchangeStatus(targetOrder.status)
                if (mappedStatus == RealOrderStatus.EXECUTED) {
                    return handleExecutedOrder(currentState, latestOrder, client)
                } else {
                    logger.info { "リアル取引: 注文 (orderId: ${latestOrder.orderId}) は未約定です。ステータス: ${targetOrder.status}" }
                    val updatedOrder = latestOrder.copy(status = mappedStatus)
                    return currentState.copy(realTrading = currentState.realTrading.copy(latestOrder = updatedOrder))
                }
            } else {
                logger.warn { "リアル取引: 注文状態の取得結果に orderId: ${latestOrder.orderId} が含まれていませんでした。" }
                val updatedOrder = latestOrder.copy(status = RealOrderStatus.UNCONFIRMED)
                return currentState.copy(realTrading = currentState.realTrading.copy(latestOrder = updatedOrder))
            }
        } catch (e: Exception) {
            logger.error(e) { "リアル取引: 注文状態の確認中にエラーが発生しました。" }
            return stopRealTrading(currentState, e.message ?: "Failed to check order status")
        }
    }

    /**
     * 約定済みの注文情報を取得し、状態に反映する
     */
    private suspend fun handleExecutedOrder(
        currentState: SimulationState,
        latestOrder: RealOrderState,
        client: RealTradingClient
    ): SimulationState {
        logger.info { "リアル取引: 注文 (orderId: ${latestOrder.orderId}) の約定を確認しました。約定情報を取得します。" }
        val executions = client.getExecutions(latestOrder.orderId)
        if (executions.isNotEmpty()) {
            var totalSize = BigDecimal.ZERO
            var totalCost = BigDecimal.ZERO
            var maxTimestamp = ""
            for (execution in executions) {
                totalSize = totalSize.add(execution.actualSize)
                totalCost = totalCost.add(execution.actualSize.multiply(execution.actualPrice))
                if (maxTimestamp.isEmpty() || execution.timestamp > maxTimestamp) {
                    maxTimestamp = execution.timestamp
                }
            }
            val averagePrice = if (totalSize > BigDecimal.ZERO) totalCost.divide(totalSize, 8, RoundingMode.HALF_UP) else BigDecimal.ZERO

            if (totalSize > BigDecimal.ZERO) {
                val updatedOrder = latestOrder.copy(
                    status = RealOrderStatus.EXECUTED,
                    executedPrice = averagePrice,
                    executedSize = totalSize,
                    executedAt = maxTimestamp
                )
                val updatedRealTrading = currentState.realTrading.copy(latestOrder = updatedOrder)

                logger.info { "リアル取引: 約定情報を反映します。isHolding=true, price=$averagePrice, size=$totalSize" }
                return currentState.copy(
                    isHolding = true,
                    buyPrice = averagePrice,
                    holdingAmount = totalSize,
                    realTrading = updatedRealTrading
                )
            } else {
                logger.warn { "リアル取引: 注文ステータスは EXECUTED ですが、約定数量が0以下でした。totalSize=$totalSize" }
                val updatedOrder = latestOrder.copy(status = RealOrderStatus.UNCONFIRMED)
                return currentState.copy(realTrading = currentState.realTrading.copy(latestOrder = updatedOrder))
            }
        } else {
            logger.warn { "リアル取引: 注文ステータスは EXECUTED ですが、約定情報が取得できませんでした。" }
            val updatedOrder = latestOrder.copy(status = RealOrderStatus.UNCONFIRMED)
            return currentState.copy(realTrading = currentState.realTrading.copy(latestOrder = updatedOrder))
        }
    }

    /**
     * BUY_CANDIDATE の場合の買い注文処理を実行する
     */
    private suspend fun executeBuyCandidateOrder(
        config: RealTradingConfig,
        tradeAmount: Int,
        symbol: String,
        currentState: SimulationState,
        currentPrice: BigDecimal
    ): SimulationState {
        logger.info { "リアル取引: BUY_CANDIDATE を検知しました。安全チェックを開始します。" }

        try {
            val client = exchangeClient ?: return currentState
            val currentHoldingAssets = client.getAssets()

            if (!checkJpyBalance(currentHoldingAssets, tradeAmount)) {
                return currentState
            }

            val isHoldingCrypto = currentHoldingAssets.any { it.symbol == symbol && it.amount > BigDecimal.ZERO }
            val checkAssets = if (isHoldingCrypto) {
                currentHoldingAssets.filter { it.symbol == symbol }
            } else {
                emptyList()
            }

            val activeOrders = client.getActiveOrders(symbol)
            val safetyCheckResult = safetyChecker.checkPreOrderSafety(
                config = config,
                tradeAmount = tradeAmount,
                state = currentState,
                currentHoldingAssets = checkAssets,
                activeOrders = activeOrders,
                currentPrice = currentPrice
            )

            if (!safetyCheckResult.passed) {
                logger.info { "リアル取引: 安全チェックで注文が見送られました。理由: ${safetyCheckResult.reason}" }
                return currentState
            }

            logger.info { "リアル取引: 安全チェックを通過しました。注文処理を開始します。" }

            val size = calculateOrderSize(tradeAmount, currentPrice)
            val acceptedOrder = client.placeOrder(
                symbol = symbol,
                side = "BUY",
                executionType = "MARKET",
                size = size,
                price = null
            )

            return buildOrderedState(
                currentState = currentState,
                orderId = acceptedOrder.orderId,
                symbol = symbol,
                tradeAmount = tradeAmount,
                size = size,
                currentPrice = currentPrice
            )
        } catch (e: Exception) {
            logger.error(e) { "リアル取引: 注文処理中にエラーが発生しました。" }
            return stopRealTrading(currentState, e.message ?: "Unknown error")
        }
    }

    /**
     * JPY残高が注文予定額を満たしているかチェックする
     */
    private fun checkJpyBalance(assets: List<cryptoautotrading.domain.model.order.ExchangeAsset>, tradeAmount: Int): Boolean {
        val jpyAsset = assets.find { it.symbol == "JPY" }
        val jpyAvailable = jpyAsset?.available ?: BigDecimal.ZERO

        if (jpyAvailable < BigDecimal(tradeAmount)) {
            val reason = "JPY available (${jpyAvailable}) が注文予定額 ($tradeAmount) 未満です"
            logger.warn { "安全チェックNG: $reason" }
            return false
        }
        return true
    }

    /**
     * 注文数量を計算する (注文予定額 / 現在価格、切り捨て)
     */
    private fun calculateOrderSize(tradeAmount: Int, currentPrice: BigDecimal): BigDecimal {
        return BigDecimal(tradeAmount).divide(currentPrice, 8, RoundingMode.DOWN)
    }

    /**
     * 注文受付後の RealOrderState と SimulationState を作成する
     */
    private fun buildOrderedState(
        currentState: SimulationState,
        orderId: String,
        symbol: String,
        tradeAmount: Int,
        size: BigDecimal,
        currentPrice: BigDecimal
    ): SimulationState {
        val nowStr = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val todayStr = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val isSameDay = currentState.realTrading.dailyOrderedDate == todayStr
        val newDailyOrderedJpy = if (isSameDay) {
            currentState.realTrading.dailyOrderedJpy.add(BigDecimal(tradeAmount))
        } else {
            BigDecimal(tradeAmount)
        }

        val newLatestOrder = RealOrderState(
            orderId = orderId,
            symbol = symbol,
            side = RealOrderSide.BUY,
            status = RealOrderStatus.ORDERED,
            requestedAmountJpy = BigDecimal(tradeAmount),
            requestedSize = size,
            requestedPrice = currentPrice,
            orderedAt = nowStr
        )

        val newRealTradingState = currentState.realTrading.copy(
            latestOrder = newLatestOrder,
            dailyOrderedDate = todayStr,
            dailyOrderedJpy = newDailyOrderedJpy
        )

        logger.info { "リアル取引: 注文受付完了。orderId: ${orderId} を保存しました。" }

        return currentState.copy(
            realTrading = newRealTradingState
        )
    }

    /**
     * エラー時にリアル取引を停止モードにする
     */
    private fun stopRealTrading(currentState: SimulationState, reason: String): SimulationState {
        val newRealTradingState = currentState.realTrading.copy(
            isStopped = true,
            stopReason = reason,
            stoppedAt = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        return currentState.copy(realTrading = newRealTradingState)
    }

    /**
     * 取引所から返却された注文ステータスの文字列を内部の RealOrderStatus 列挙型に変換する。
     *
     * @param status 取引所側のステータス文字列
     * @return 変換後のステータス
     */
    private fun mapExchangeStatus(status: String): RealOrderStatus {
        return when (status) {
            "WAITING" -> RealOrderStatus.WAITING
            "ORDERED" -> RealOrderStatus.ORDERED
            "CANCELED" -> RealOrderStatus.CANCELED
            "EXECUTED" -> RealOrderStatus.EXECUTED
            else -> RealOrderStatus.UNCONFIRMED
        }
    }
}
