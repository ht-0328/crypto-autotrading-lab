package cryptoautotrading.domain.realtrading

import cryptoautotrading.domain.model.OrderSizingMode
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.realtrading.RealOrderSide
import cryptoautotrading.domain.model.realtrading.RealOrderState
import cryptoautotrading.domain.model.realtrading.RealOrderStatus
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.order.ExecutedOrder
import cryptoautotrading.domain.model.realtrading.ExecutionSummary
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.domain.model.realtrading.RealTradingState
import cryptoautotrading.domain.realtrading.RealTradingClient
import cryptoautotrading.domain.time.TradingTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * リアル取引の注文処理を担当するサービス
 *
 * @property exchangeClient リアル取引の取引所操作を行うクライアント
 * @property safetyChecker 実注文前安全チェックを行うサービス
 * @property clock 注文時刻と日次上限の日付判定に使う時計。テストでは固定した時刻に差し替える
 */
class RealTradingService(
    private val exchangeClient: RealTradingClient? = null,
    private val safetyChecker: RealTradingSafetyChecker = RealTradingSafetyChecker(),
    private val clock: Clock = TradingTime.systemClock()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 判定結果と設定に基づいてリアル注文を実行するかどうかを判断し、処理を行う。
     *
     * BUY_CANDIDATE / SELL_CANDIDATE の場合、安全チェックを通過した場合のみ GMO Private API の
     * placeOrder を呼び、返ってきた orderId を state の realTrading.latestOrder に保存する。
     *
     * 注文の受付と約定は別のため、ここでは保有状態を変更しない。約定の反映は次回以降の実行で
     * [checkLatestOrderStatus] が行う。
     *
     * @param decision 戦略判定によって下された売買判定結果
     * @param config リアル取引に関する設定
     * @param tradeAmount 1回あたりの注文予定金額(JPY)
     * @param symbol 銘柄名
     * @param currentState 現在のシミュレーション(およびリアル取引)の状態
     * @param klineClosePrice 売買判定に使ったK線の終値。取引所の最新価格と比べるための基準として使う
     * @param tickerPrice 取引所から取得した最新価格。注文数量と注文金額の計算にはこちらを使う。
     *   取得できていない場合は null
     * @param orderSizingMode 注文サイズの決め方
     * @return 処理後のシミュレーション状態
     */
    suspend fun executeOrderIfNeeded(
        decision: TradeDecision,
        config: RealTradingConfig,
        tradeAmount: Int,
        symbol: String,
        currentState: SimulationState,
        klineClosePrice: BigDecimal,
        tickerPrice: BigDecimal?,
        orderSizingMode: OrderSizingMode = OrderSizingMode.FIXED_AMOUNT
    ): SimulationState {
        if (shouldSkipRealTrading(config)) {
            return currentState
        }

        if (handleMissingExchangeClient(decision.action)) {
            return currentState
        }

        // 未確認注文の照合は、価格が使えるかどうかに関係なく先に行う
        if (hasUnconfirmedOrder(currentState)) {
            return checkLatestOrderStatus(currentState, config)
        }

        if (decision.action != TradeAction.BUY_CANDIDATE && decision.action != TradeAction.SELL_CANDIDATE) {
            logger.debug { "売買判定が注文対象ではないため、リアル取引は実行しません。action=${decision.action}" }
            return currentState
        }

        val orderPrice = try {
            resolveOrderPrice(config, klineClosePrice, tickerPrice) ?: return currentState
        } catch (e: Exception) {
            // 通常は起動時ガードで弾かれる設定漏れ。万一到達したら安全側に止める
            logger.error(e) { "リアル取引: 注文価格の決定に失敗しました。" }
            return stopRealTrading(currentState, e.message ?: "注文価格の決定に失敗しました")
        }

        return when (decision.action) {
            TradeAction.BUY_CANDIDATE -> executeBuyCandidateOrder(
                config = config,
                tradeAmount = tradeAmount,
                symbol = symbol,
                currentState = currentState,
                currentPrice = orderPrice,
                orderSizingMode = orderSizingMode
            )
            else -> executeSellCandidateOrder(
                config = config,
                symbol = symbol,
                currentState = currentState,
                currentPrice = orderPrice
            )
        }
    }

    /**
     * 注文に使う価格を決める。
     *
     * K線の終値は最大で1本分（5分足なら5分）古い。急騰しているときにその価格で
     * 「注文金額 ÷ 価格」を計算すると、実際より多い数量を注文することになり、
     * 約定金額が上限を超える。そのため注文には取引所の最新価格を使う。
     *
     * 最新価格が取得できない、または判定に使ったK線の終値から大きく離れている場合は、
     * どちらの価格が正しいか判断できないため注文を見送る。
     *
     * @param config リアル取引設定
     * @param klineClosePrice 売買判定に使ったK線の終値
     * @param tickerPrice 取引所から取得した最新価格
     * @return 注文に使う価格。使える価格が無い場合は null
     */
    private fun resolveOrderPrice(
        config: RealTradingConfig,
        klineClosePrice: BigDecimal,
        tickerPrice: BigDecimal?
    ): BigDecimal? {
        if (tickerPrice == null || tickerPrice <= BigDecimal.ZERO) {
            logger.warn { "リアル取引: 取引所の最新価格が取得できていないため、注文を見送ります。tickerPrice=$tickerPrice" }
            return null
        }

        val orderPriceSpec = resolveOrderPriceSpec(config)
        if (!orderPriceSpec.isWithinAllowedSlippage(klineClosePrice, tickerPrice)) {
            val divergenceRate = orderPriceSpec.calculateDivergenceRate(klineClosePrice, tickerPrice)
            logger.warn {
                "リアル取引: K線の終値 ($klineClosePrice) と取引所の最新価格 ($tickerPrice) の乖離 " +
                    "($divergenceRate) が許容範囲 (${orderPriceSpec.maxSlippageRate}) を超えたため、注文を見送ります。"
            }
            return null
        }

        return tickerPrice
    }

    /**
     * 設定から注文価格と手数料の制約を組み立てる。
     *
     * @param config リアル取引設定
     * @return 注文価格の制約
     * @throws IllegalStateException 手数料率または許容スリッページが未設定の場合
     */
    private fun resolveOrderPriceSpec(config: RealTradingConfig): OrderPriceSpec {
        val takerFeeRate = config.takerFeeRate
            ?: error("taker_fee_rate が未設定です。実注文には取引所の手数料率の設定が必要です")
        val maxSlippageRate = config.maxSlippageRate
            ?: error("max_slippage_rate が未設定です。実注文には許容スリッページの設定が必要です")
        return OrderPriceSpec(takerFeeRate = takerFeeRate, maxSlippageRate = maxSlippageRate)
    }

    /**
     * リアル取引をスキップすべきかどうかを判定する。
     *
     * @param config リアル取引設定
     * @return リアル取引をスキップする場合は true
     */
    private fun shouldSkipRealTrading(config: RealTradingConfig): Boolean {
        if (config.dryRun || !config.realTradeEnabled) {
            logger.debug { "リアル取引が無効、または dry-run モードのため、実注文処理をスキップします。" }
            return true
        }
        return false
    }

    /**
     * exchangeClient が未設定の場合の処理を行う。
     *
     * @param action 取引アクション
     * @return スキップすべき場合は true
     */
    private fun handleMissingExchangeClient(action: TradeAction): Boolean {
        if (exchangeClient == null) {
            if (action == TradeAction.BUY_CANDIDATE || action == TradeAction.SELL_CANDIDATE) {
                logger.warn { "リアル取引: exchangeClient が設定されていないため、実注文処理をスキップします。" }
            }
            return true
        }
        return false
    }

    /**
     * 未確認注文の状態かどうかを判定する。
     *
     * @return 未確認注文の状態である場合は true
     */
    private fun RealOrderStatus.isWaitingForConfirmation(): Boolean {
        return this == RealOrderStatus.ORDERED ||
               this == RealOrderStatus.WAITING ||
               this == RealOrderStatus.UNCONFIRMED
    }

    /**
     * 状態に未確認の注文が存在するかどうかを判定する。
     *
     * @param currentState 現在のシミュレーション状態
     * @return 未確認の注文が存在する場合は true
     */
    private fun hasUnconfirmedOrder(currentState: SimulationState): Boolean {
        val latestOrder = currentState.realTrading.latestOrder ?: return false
        return latestOrder.status.isWaitingForConfirmation()
    }

    /**
     * 注文を未確認状態に戻す。
     *
     * @param currentState 現在のシミュレーション状態
     * @param latestOrder 最新の注文状態
     * @return 更新されたシミュレーション状態
     */
    private fun markLatestOrderUnconfirmed(
        currentState: SimulationState,
        latestOrder: RealOrderState
    ): SimulationState {
        val updatedOrder = latestOrder.copy(status = RealOrderStatus.UNCONFIRMED)
        return currentState.copy(realTrading = currentState.realTrading.copy(latestOrder = updatedOrder))
    }

    /**
     * 注文状態を更新する。
     *
     * @param currentState 現在のシミュレーション状態
     * @param latestOrder 最新の注文状態
     * @param status 新しいステータス
     * @return 更新されたシミュレーション状態
     */
    private fun updateLatestOrderStatus(
        currentState: SimulationState,
        latestOrder: RealOrderState,
        status: RealOrderStatus
    ): SimulationState {
        val updatedOrder = latestOrder.copy(status = status)
        return currentState.copy(realTrading = currentState.realTrading.copy(latestOrder = updatedOrder))
    }

    /**
     * 未確認の最新注文の状態を確認し、シミュレーション状態に反映する。
     *
     * @param currentState 現在のシミュレーション状態
     * @param config リアル取引設定
     * @return 更新されたシミュレーション状態
     */
    private suspend fun checkLatestOrderStatus(
        currentState: SimulationState,
        config: RealTradingConfig
    ): SimulationState {
        val latestOrder = currentState.realTrading.latestOrder ?: return currentState
        logger.info { "リアル取引: 未確認注文 (orderId: ${latestOrder.orderId}) が存在します。状態を確認します。" }

        try {
            val client = exchangeClient ?: return currentState
            val orders = client.getOrders(latestOrder.orderId)
            val targetOrder = orders.find { it.orderId == latestOrder.orderId }

            if (targetOrder == null) {
                logger.warn { "リアル取引: 注文状態の取得結果に orderId: ${latestOrder.orderId} が含まれていませんでした。" }
                return markLatestOrderUnconfirmed(currentState, latestOrder)
            }

            val mappedStatus = mapExchangeStatus(targetOrder.status)
            if (mappedStatus == RealOrderStatus.EXECUTED) {
                return handleExecutedOrder(currentState, latestOrder, client, config)
            }

            logger.info { "リアル取引: 注文 (orderId: ${latestOrder.orderId}) は未約定です。ステータス: ${targetOrder.status}" }
            return updateLatestOrderStatus(currentState, latestOrder, mappedStatus)
        } catch (e: Exception) {
            logger.error(e) { "リアル取引: 注文状態の確認中にエラーが発生しました。" }
            return stopRealTrading(currentState, e.message ?: "注文状態の確認に失敗しました")
        }
    }

    /**
     * 約定情報を集計する。
     *
     * @param executions 約定情報のリスト
     * @return 集計結果
     */
    private fun summarizeExecutions(executions: List<ExecutedOrder>): ExecutionSummary {
        return executions.fold(ExecutionSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "")) { acc, execution ->
            val newTotalSize = acc.totalSize.add(execution.actualSize)
            val newTotalCost = acc.totalCost.add(execution.actualSize.multiply(execution.actualPrice))
            val newTotalFee = acc.totalFee.add(execution.fee)
            val newLatestTimestamp = if (acc.latestTimestamp.isEmpty() || execution.timestamp > acc.latestTimestamp) {
                execution.timestamp
            } else {
                acc.latestTimestamp
            }
            ExecutionSummary(newTotalSize, newTotalCost, newTotalFee, newLatestTimestamp)
        }
    }

    /**
     * 約定済みの注文情報を取得し、状態に反映する。
     *
     * @param currentState 現在のシミュレーション状態
     * @param latestOrder 最新の注文状態
     * @param client リアル取引クライアント
     * @param config リアル取引設定
     * @return 更新されたシミュレーション状態
     */
    private suspend fun handleExecutedOrder(
        currentState: SimulationState,
        latestOrder: RealOrderState,
        client: RealTradingClient,
        config: RealTradingConfig
    ): SimulationState {
        logger.info { "リアル取引: 注文 (orderId: ${latestOrder.orderId}) の約定を確認しました。約定情報を取得します。" }
        val executions = client.getExecutions(latestOrder.orderId)

        if (executions.isEmpty()) {
            logger.warn { "リアル取引: 注文ステータスは EXECUTED ですが、約定情報が取得できませんでした。" }
            return markLatestOrderUnconfirmed(currentState, latestOrder)
        }

        val summary = summarizeExecutions(executions)

        if (summary.totalSize <= BigDecimal.ZERO) {
            logger.warn { "リアル取引: 注文ステータスは EXECUTED ですが、約定数量が0以下でした。totalSize=${summary.totalSize}" }
            return markLatestOrderUnconfirmed(currentState, latestOrder)
        }

        val averagePrice = summary.totalCost.divide(summary.totalSize, 8, RoundingMode.HALF_UP)
        val updatedOrder = latestOrder.copy(
            status = RealOrderStatus.EXECUTED,
            executedPrice = averagePrice,
            executedSize = summary.totalSize,
            executedAt = summary.latestTimestamp
        )
        val updatedRealTrading = currentState.realTrading.copy(latestOrder = updatedOrder)

        // 売買区分によって保有状態への反映が正反対になる。買いの約定を売りに流用すると
        // 「売ったのに保有中」になり、以降の損切り判断がすべて狂う。
        val executedState = when (latestOrder.side) {
            RealOrderSide.BUY -> applyExecutedBuy(currentState, updatedRealTrading, averagePrice, summary)
            RealOrderSide.SELL -> applyExecutedSell(currentState, updatedRealTrading, averagePrice, summary)
        }

        return stopIfSlippageExceeded(executedState, latestOrder, averagePrice, config)
    }

    /**
     * 約定価格が発注時の想定から離れすぎていた場合に、新規の買いを止める。
     *
     * 成行注文は板が薄いときや急変時に想定と違う価格で約定する。乖離が続く状況で
     * 注文を出し続けると、想定していない価格で売買を繰り返すことになる。
     *
     * 止めるのは新規の買いだけである。保有を解消する売りは止めない。
     *
     * @param currentState 約定を反映済みのシミュレーション状態
     * @param latestOrder 約定した注文
     * @param averagePrice 平均約定価格
     * @param config リアル取引設定
     * @return 乖離が許容範囲を超えていれば停止させた状態、そうでなければ入力のままの状態
     */
    private fun stopIfSlippageExceeded(
        currentState: SimulationState,
        latestOrder: RealOrderState,
        averagePrice: BigDecimal,
        config: RealTradingConfig
    ): SimulationState {
        val requestedPrice = latestOrder.requestedPrice
        if (requestedPrice == null || requestedPrice <= BigDecimal.ZERO) {
            return currentState
        }

        val orderPriceSpec = resolveOrderPriceSpec(config)
        if (orderPriceSpec.isWithinAllowedSlippage(requestedPrice, averagePrice)) {
            return currentState
        }

        val divergenceRate = orderPriceSpec.calculateDivergenceRate(requestedPrice, averagePrice)
        val reason = "約定価格 ($averagePrice) が発注時の想定価格 ($requestedPrice) から " +
            "$divergenceRate 乖離し、許容範囲 (${orderPriceSpec.maxSlippageRate}) を超えました"
        logger.error { "リアル取引: $reason" }
        return stopRealTrading(currentState, reason)
    }

    /**
     * 買い注文の約定を状態に反映する。
     *
     * @param currentState 現在のシミュレーション状態
     * @param updatedRealTrading 約定を反映済みのリアル取引状態
     * @param averagePrice 平均約定価格
     * @param summary 約定情報の集計結果
     * @return 更新されたシミュレーション状態
     */
    private fun applyExecutedBuy(
        currentState: SimulationState,
        updatedRealTrading: RealTradingState,
        averagePrice: BigDecimal,
        summary: ExecutionSummary
    ): SimulationState {
        logger.info { "リアル取引: 買いの約定を反映します。isHolding=true, price=$averagePrice, size=${summary.totalSize}" }
        return currentState.copy(
            isHolding = true,
            buyPrice = averagePrice,
            holdingAmount = summary.totalSize,
            realTrading = updatedRealTrading
        )
    }

    /**
     * 売り注文の約定を状態に反映する。
     *
     * 確定損益は「売却代金 - 取得原価 - 売却時の手数料」で計算する。買い時の手数料は
     * `buyPrice`（平均約定価格）に含まれないため、この損益には反映されない。
     *
     * @param currentState 現在のシミュレーション状態
     * @param updatedRealTrading 約定を反映済みのリアル取引状態
     * @param averagePrice 平均約定価格
     * @param summary 約定情報の集計結果
     * @return 更新されたシミュレーション状態
     */
    private fun applyExecutedSell(
        currentState: SimulationState,
        updatedRealTrading: RealTradingState,
        averagePrice: BigDecimal,
        summary: ExecutionSummary
    ): SimulationState {
        val proceeds = summary.totalCost.subtract(summary.totalFee)
        val acquisitionCost = currentState.buyPrice.multiply(summary.totalSize)
        val profitAndLoss = proceeds.subtract(acquisitionCost)

        // 部分約定に備えて残量から保有継続を判断する。全量売れていれば残量は0になる。
        val remainingSize = currentState.holdingAmount.subtract(summary.totalSize).max(BigDecimal.ZERO)
        val isStillHolding = remainingSize > BigDecimal.ZERO

        logger.info {
            "リアル取引: 売りの約定を反映します。price=$averagePrice, size=${summary.totalSize}, " +
                "確定損益=$profitAndLoss, 残量=$remainingSize"
        }

        // 1日の損失上限と連敗の判定に使うため、確定した損益をその日の集計に足す
        val realTradingWithResult = updatedRealTrading.withRealizedResult(
            date = resolveTodayString(),
            profitAndLoss = profitAndLoss
        )

        return currentState.copy(
            isHolding = isStillHolding,
            buyPrice = if (isStillHolding) currentState.buyPrice else BigDecimal.ZERO,
            holdingAmount = remainingSize,
            cashBalance = currentState.cashBalance.add(proceeds),
            realizedProfitAndLoss = currentState.realizedProfitAndLoss.add(profitAndLoss),
            entryAtr = if (isStillHolding) currentState.entryAtr else null,
            realTrading = realTradingWithResult
        )
    }

    /**
     * BUY_CANDIDATE の場合の買い注文処理を実行する。
     *
     * @param config リアル取引設定
     * @param tradeAmount 注文金額
     * @param symbol 通貨ペアシンボル
     * @param currentState 現在のシミュレーション状態
     * @param currentPrice 現在の価格
     * @return 更新されたシミュレーション状態
     */
    private suspend fun executeBuyCandidateOrder(
        config: RealTradingConfig,
        tradeAmount: Int,
        symbol: String,
        currentState: SimulationState,
        currentPrice: BigDecimal,
        orderSizingMode: OrderSizingMode
    ): SimulationState {
        logger.info { "リアル取引: BUY_CANDIDATE を検知しました。安全チェックを開始します。" }

        try {
            val client = exchangeClient ?: return currentState
            val orderSizeSpec = resolveOrderSizeSpec(config)
            val orderPriceSpec = resolveOrderPriceSpec(config)
            val currentHoldingAssets = client.getAssets()

            val targetOrderAmount = when (orderSizingMode) {
                OrderSizingMode.FIXED_AMOUNT -> tradeAmount
                OrderSizingMode.ALL_IN -> {
                    val jpyAsset = currentHoldingAssets.find { it.symbol == "JPY" }
                    val jpyAvailable = jpyAsset?.available ?: BigDecimal.ZERO
                    if (jpyAvailable <= BigDecimal.ZERO) {
                        logger.warn { "安全チェックNG: JPY available ($jpyAvailable) は0以下です" }
                        return currentState
                    }
                    // 残高を全額注文に回すと手数料の分だけ足りなくなるため、手数料を差し引いた額にする
                    orderPriceSpec.calculateAffordableOrderAmount(jpyAvailable)
                }
            }

            // 上限は「実際に口座から出ていく額」に対してかける必要があるため、手数料を含めて判定する
            val totalCostWithFee = orderPriceSpec.calculateTotalCostWithFee(targetOrderAmount)

            if (!checkJpyBalance(currentHoldingAssets, totalCostWithFee)) {
                return currentState
            }

            // 手数料や丸めで残る端数（ダスト）を保有とみなすと、二重保有防止のチェックに
            // 永久に引っかかり、以後1回も買えなくなる。最小注文数量を保有の閾値とする。
            val isHoldingCrypto = currentHoldingAssets.any {
                it.symbol == symbol && orderSizeSpec.isHoldingAmount(it.amount)
            }
            val checkAssets = if (isHoldingCrypto) {
                currentHoldingAssets.filter { it.symbol == symbol }
            } else {
                emptyList()
            }

            val activeOrders = client.getActiveOrders(symbol)
            val safetyCheckResult = safetyChecker.checkPreOrderSafety(
                config = config,
                tradeAmount = totalCostWithFee,
                state = currentState,
                currentHoldingAssets = checkAssets,
                activeOrders = activeOrders,
                currentPrice = currentPrice,
                today = resolveTodayString()
            )

            if (!safetyCheckResult.passed) {
                logger.info { "リアル取引: 安全チェックで注文が見送られました。理由: ${safetyCheckResult.reason}" }
                return currentState
            }

            logger.info { "リアル取引: 安全チェックを通過しました。注文処理を開始します。" }

            val size = calculateOrderSize(targetOrderAmount, currentPrice, orderSizeSpec)
            if (!orderSizeSpec.isTradable(size)) {
                // 見送りであって異常ではないため、停止させずに次回に回す
                logger.info {
                    "リアル取引: 注文数量 ($size) が最小注文数量 (${orderSizeSpec.minOrderSize}) に満たないため、" +
                        "買い注文を見送ります。注文予定額=$targetOrderAmount, 価格=$currentPrice"
                }
                return currentState
            }

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
                tradeAmount = totalCostWithFee,
                size = size,
                currentPrice = currentPrice
            )
        } catch (e: Exception) {
            logger.error(e) { "リアル取引: 注文処理中にエラーが発生しました。" }
            return stopRealTrading(currentState, e.message ?: "不明なエラーが発生しました")
        }
    }

    /**
     * SELL_CANDIDATE の場合の売り注文処理を実行する。
     *
     * 保有解消は損失を止めるための操作なので、`realTrading.isStopped` でも実行する。
     * 停止中に売りまで止めると、ポジションを抱えたまま損切りできなくなるためである。
     * 停止が止めるのは新規の買いだけである。
     *
     * @param config リアル取引設定
     * @param symbol 通貨ペアシンボル
     * @param currentState 現在のシミュレーション状態
     * @param currentPrice 現在の市場価格
     * @return 更新されたシミュレーション状態
     */
    private suspend fun executeSellCandidateOrder(
        config: RealTradingConfig,
        symbol: String,
        currentState: SimulationState,
        currentPrice: BigDecimal
    ): SimulationState {
        logger.info { "リアル取引: SELL_CANDIDATE を検知しました。安全チェックを開始します。" }

        try {
            val client = exchangeClient ?: return currentState
            val orderSizeSpec = resolveOrderSizeSpec(config)

            val recordedHoldingSize = currentState.holdingAmount
            if (recordedHoldingSize <= BigDecimal.ZERO) {
                logger.info { "リアル取引: 記録上の保有数量が0以下のため、売り注文は行いません。" }
                return currentState
            }

            val assets = client.getAssets()
            val exchangeAvailableSize = assets.find { it.symbol == symbol }?.available ?: BigDecimal.ZERO

            // 取引所の残高が記録より少ない場合、このアプリの知らないところで資産が動いている。
            // そのまま売ると状態が食い違ったまま進むため、停止して人の確認を待つ。
            if (exchangeAvailableSize < recordedHoldingSize) {
                val reason = "取引所の売却可能残高 ($exchangeAvailableSize) が記録上の保有数量 " +
                    "($recordedHoldingSize) より少ないため、売り注文を中止しました"
                logger.error { "リアル取引: $reason" }
                return stopRealTrading(currentState, reason)
            }

            // 同じ口座にこのアプリ以外が買った資産があっても巻き込まないよう、
            // 取引所の残高ではなく記録上の保有数量を売る。刻みに合わない数量は拒否されるため丸める。
            val sellSize = orderSizeSpec.roundDownToStep(recordedHoldingSize)
            if (!orderSizeSpec.isTradable(sellSize)) {
                // 保有量がダストしか残っていない状態。売れないが異常ではないため停止させない。
                logger.info {
                    "リアル取引: 売却数量 ($sellSize) が最小注文数量 (${orderSizeSpec.minOrderSize}) に" +
                        "満たないため、売り注文を見送ります。記録上の保有数量=$recordedHoldingSize"
                }
                return currentState
            }

            val activeOrders = client.getActiveOrders(symbol)
            val safetyCheckResult = safetyChecker.checkPreSellOrderSafety(
                sellSize = sellSize,
                recordedHoldingSize = recordedHoldingSize,
                exchangeAvailableSize = exchangeAvailableSize,
                state = currentState,
                activeOrders = activeOrders
            )

            if (!safetyCheckResult.passed) {
                logger.info { "リアル取引: 安全チェックで売り注文が見送られました。理由: ${safetyCheckResult.reason}" }
                return currentState
            }

            logger.info { "リアル取引: 売りの安全チェックを通過しました。注文処理を開始します。" }

            val acceptedOrder = client.placeOrder(
                symbol = symbol,
                side = "SELL",
                executionType = "MARKET",
                size = sellSize,
                price = null
            )

            return buildSellOrderedState(
                currentState = currentState,
                orderId = acceptedOrder.orderId,
                symbol = symbol,
                size = sellSize,
                currentPrice = currentPrice
            )
        } catch (e: Exception) {
            logger.error(e) { "リアル取引: 売り注文の処理中にエラーが発生しました。" }
            return stopRealTrading(currentState, e.message ?: "売り注文の処理中に不明なエラーが発生しました")
        }
    }

    /**
     * 売り注文の受付後の RealOrderState を作成し、状態に反映する。
     *
     * 日次の注文上限（`dailyOrderedJpy`）は新規の買いによる露出を絞るためのものなので、
     * 保有を解消する売りは加算しない。
     *
     * @param currentState 現在のシミュレーション状態
     * @param orderId 注文ID
     * @param symbol 通貨ペアシンボル
     * @param size 注文数量
     * @param currentPrice 現在の市場価格
     * @return 更新されたシミュレーション状態
     */
    private fun buildSellOrderedState(
        currentState: SimulationState,
        orderId: String,
        symbol: String,
        size: BigDecimal,
        currentPrice: BigDecimal
    ): SimulationState {
        val nowStr = LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val newLatestOrder = RealOrderState(
            orderId = orderId,
            symbol = symbol,
            side = RealOrderSide.SELL,
            status = RealOrderStatus.ORDERED,
            requestedAmountJpy = size.multiply(currentPrice),
            requestedSize = size,
            requestedPrice = currentPrice,
            orderedAt = nowStr
        )

        logger.info { "リアル取引: 売り注文の受付完了。orderId: $orderId を保存しました。" }

        return currentState.copy(
            realTrading = currentState.realTrading.copy(latestOrder = newLatestOrder)
        )
    }

    /**
     * 時計から今日の日付を返す。
     *
     * @return ISO形式の日付文字列
     */
    private fun resolveTodayString(): String {
        return LocalDateTime.now(clock).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * JPY残高が注文予定額を満たしているかチェックする。
     *
     * @param assets 資産リスト
     * @param tradeAmount 注文金額
     * @return JPY残高が注文予定額を満たしている場合は true
     */
    private fun checkJpyBalance(assets: List<ExchangeAsset>, tradeAmount: Int): Boolean {
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
     * 注文数量を計算する (注文予定額 / 現在価格を、取引所の刻みに切り捨て)。
     *
     * 刻みに丸めないと、取引所が受け付けない数量を送ることになり注文が拒否される。
     *
     * @param tradeAmount 注文金額
     * @param currentPrice 現在の価格
     * @param orderSizeSpec 取引所の注文数量の制約
     * @return 取引所に送れる注文数量
     */
    private fun calculateOrderSize(
        tradeAmount: Int,
        currentPrice: BigDecimal,
        orderSizeSpec: OrderSizeSpec
    ): BigDecimal {
        val rawSize = BigDecimal(tradeAmount).divide(currentPrice, SIZE_CALCULATION_SCALE, RoundingMode.DOWN)
        return orderSizeSpec.roundDownToStep(rawSize)
    }

    /**
     * 設定から取引所の注文数量の制約を組み立てる。
     *
     * 未設定のまま実注文を行うと刻みに合わない数量を送ることになるため、
     * 起動時ガード([cryptoautotrading.presentation]) で弾く前提で必須としている。
     *
     * @param config リアル取引設定
     * @return 注文数量の制約
     * @throws IllegalStateException 最小注文数量または刻みが未設定の場合
     */
    private fun resolveOrderSizeSpec(config: RealTradingConfig): OrderSizeSpec {
        val minOrderSize = config.minOrderSize
            ?: error("min_order_size が未設定です。実注文には取引所の最小注文数量の設定が必要です")
        val sizeStep = config.sizeStep
            ?: error("size_step が未設定です。実注文には取引所の注文数量の刻みの設定が必要です")
        return OrderSizeSpec(minOrderSize = minOrderSize, sizeStep = sizeStep)
    }

    /**
     * 注文受付後の RealOrderState と SimulationState を作成する。
     *
     * @param currentState 現在のシミュレーション状態
     * @param orderId 注文ID
     * @param symbol 通貨ペアシンボル
     * @param tradeAmount 注文金額
     * @param size 注文数量
     * @param currentPrice 現在の価格
     * @return 更新されたシミュレーション状態
     */
    private fun buildOrderedState(
        currentState: SimulationState,
        orderId: String,
        symbol: String,
        tradeAmount: Int,
        size: BigDecimal,
        currentPrice: BigDecimal
    ): SimulationState {
        val now = LocalDateTime.now(clock)
        val nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val todayStr = resolveTodayString()
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
     * エラー時にリアル取引を停止モードにする。
     *
     * @param currentState 現在のシミュレーション状態
     * @param reason 停止理由
     * @return 更新されたシミュレーション状態
     */
    private fun stopRealTrading(currentState: SimulationState, reason: String): SimulationState {
        val newRealTradingState = currentState.realTrading.copy(
            isStopped = true,
            stopReason = reason,
            stoppedAt = LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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

    private companion object {
        /** 刻みに丸める前の注文数量を計算するときの小数点以下の桁数 */
        const val SIZE_CALCULATION_SCALE = 8
    }
}
