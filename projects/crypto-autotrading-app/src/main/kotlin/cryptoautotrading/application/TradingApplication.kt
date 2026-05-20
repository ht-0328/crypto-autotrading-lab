package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.repository.MarketDataClient
import cryptoautotrading.domain.repository.ResultOutputPort
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.repository.SimulationStateRepository
import cryptoautotrading.domain.repository.TradeHistoryRepository
import cryptoautotrading.domain.simulation.ProfitAndLossCalculator
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.strategy.CooldownReboundStrategy
import cryptoautotrading.domain.strategy.SafeReboundStrategy
import cryptoautotrading.domain.strategy.SimpleContrarianStrategy
import cryptoautotrading.domain.strategy.TrendConfirmReboundStrategy
import cryptoautotrading.domain.strategy.AtrTrendConfirmReboundStrategy
import cryptoautotrading.domain.strategy.TradingStrategy
import cryptoautotrading.application.port.RealTradingExchangePort
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * トレーディングアプリケーションのメインロジックを実行するクラス
 *
 * @property config アプリケーション設定
 * @property marketDataClient 市場データクライアント
 * @property stateRepository 状態リポジトリ
 * @property tradeHistoryRepository 取引履歴リポジトリ
 * @property resultOutputPort 結果出力ポート
 */
class TradingApplication(
    private val config: AppConfig,
    private val marketDataClient: MarketDataClient,
    private val stateRepository: SimulationStateRepository,
    private val tradeHistoryRepository: TradeHistoryRepository,
    private val resultOutputPort: ResultOutputPort,
    private val realTradingExchangePort: RealTradingExchangePort? = null
) {

    private val logger = KotlinLogging.logger {}
    private val simulationService = SimulationService()
    private val realTradeOrderUseCase = RealTradeOrderUseCase(exchangePort = realTradingExchangePort)
    private val pnlCalculator = ProfitAndLossCalculator()

/**
     * アプリケーションの実行を開始する
     */
    suspend fun run() {
        try {
            logger.info { "TradingApplication のメイン処理を開始します" }
            logger.debug { "実行設定: $config" }

            // 1. 状態の読み込みと初期資金の反映
            var currentState = stateRepository.load()

            val isUninitializedState = currentState.cashBalance.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.buyPrice.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.holdingAmount.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.realizedProfitAndLoss.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.lastUpdatedAt.isEmpty()

            val isLegacyHoldingState = currentState.cashBalance.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.isHolding && currentState.holdingAmount > java.math.BigDecimal.ZERO

            if (isUninitializedState || isLegacyHoldingState) {
                val initialCapital = java.math.BigDecimal(config.trading.initialCapital)
                val newCashBalance = if (currentState.isHolding) {
                    val cost = currentState.buyPrice * currentState.holdingAmount
                    val balance = initialCapital - cost
                    if (balance < java.math.BigDecimal.ZERO) java.math.BigDecimal.ZERO else balance
                } else {
                    initialCapital
                }
                currentState = currentState.copy(cashBalance = newCashBalance)
                logger.info { "初期資金を反映し、残金を $newCashBalance に設定しました" }
            }

            logger.info { "現在のシミュレーション状態を読み込みました" }
            logger.debug { "読み込んだ状態: $currentState" }

            // 2. APIからデータの取得
            val klineData = fetchKlineData()

            // 3. 売買判定
            val strategy = createStrategy(config.trading)
            val decision = strategy.judge(klineData, currentState)
            logger.info { "Trade Decision: ${decision.action.description}, Reason: ${decision.reason}" }

            // 4. 状態の更新
            // 最新のK線の終値を現在価格とする。データが空の場合は終了する
            if (klineData.isEmpty()) {
                logger.warn { "Klines data is empty. Skipping this run." }
                return
            }
            val latestKline = klineData.sortedBy { it.openTime }.last()
            val currentPrice = latestKline.close.toBigDecimal()

            // リアル取引の処理 (実注文・状態保存)
            currentState = realTradeOrderUseCase.executeOrderIfNeeded(
                decision = decision,
                config = config.realTrading,
                tradeAmount = config.trading.tradeAmount,
                symbol = config.trading.symbol,
                currentState = currentState,
                currentPrice = currentPrice
            )

            // 損益と想定損益の計算
            val pnl = pnlCalculator.calculate(
                isHolding = currentState.isHolding,
                currentPrice = currentPrice,
                buyPrice = currentState.buyPrice,
                holdingAmount = currentState.holdingAmount,
                shouldSell = decision.action == TradeAction.SELL_CANDIDATE
            )
            val fee = java.math.BigDecimal.ZERO // Phase1 では手数料ゼロとする

            val nextState = simulationService.updateState(
                currentState = currentState,
                decision = decision,
                currentPrice = currentPrice,
                tradeAmount = config.trading.tradeAmount,
                eventTime = latestKline.openTime
            )
            logger.info { "Next Simulation State: $nextState" }

            val estimatedHoldingValue = nextState.holdingAmount * currentPrice
            val totalAssetValue = nextState.cashBalance + estimatedHoldingValue

            // 5. 出力
            // コンソール出力
            resultOutputPort.printResult(
                price = currentPrice,
                action = decision.action,
                reason = decision.reason,
                profitAndLoss = pnl.profitAndLoss,
                estimatedProfitAndLoss = pnl.estimatedProfitAndLoss,
                cashBalance = nextState.cashBalance,
                holdingAmount = nextState.holdingAmount,
                buyPrice = nextState.buyPrice,
                realizedProfitAndLoss = nextState.realizedProfitAndLoss,
                estimatedHoldingValue = estimatedHoldingValue,
                totalAssetValue = totalAssetValue
            )

            // CSV出力
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            tradeHistoryRepository.append(
                datetime = nowStr,
                price = currentPrice,
                sign = decision.action.description,
                reason = decision.reason,
                profitAndLoss = pnl.profitAndLoss,
                isHolding = nextState.isHolding,
                fee = fee,
                cashBalance = nextState.cashBalance,
                holdingAmount = nextState.holdingAmount,
                buyPrice = nextState.buyPrice,
                realizedProfitAndLoss = nextState.realizedProfitAndLoss,
                estimatedHoldingValue = estimatedHoldingValue,
                totalAssetValue = totalAssetValue
            )

            // シミュレーション結果サマリーのログ出力
            logSimulationSummary(
                currentState = currentState,
                nextState = nextState,
                decision = decision,
                currentPrice = currentPrice,
                pnl = pnl,
                estimatedHoldingValue = estimatedHoldingValue,
                totalAssetValue = totalAssetValue,
                tradeAmount = config.trading.tradeAmount
            )

            // 6. 状態の保存
            stateRepository.save(nextState)

            logger.info { "TradingApplication のメイン処理が正常に完了しました" }

        } catch (e: Exception) {
            logger.error(e) { "TradingApplication の実行中にエラーが発生しました: ${e.message}" }
            throw e
        }
    }

/**
     * 取引所のAPIから最新のティッカー情報とK線（ローソク足）データを取得する。
     *
     * @return 取得したK線データのリスト
     */
    private suspend fun fetchKlineData(): List<Kline> {
        val tickerResponse = marketDataClient.getTicker(config.trading.symbol)
        val ticker = tickerResponse.data.firstOrNull()
        logger.debug { "取得したティッカー主要値: symbol=${ticker?.symbol}, last=${ticker?.last}, bid=${ticker?.bid}, ask=${ticker?.ask}" }

        val targetDate = resolveKlineTargetDate()
        val klineResponse = marketDataClient.getKlines(config.trading.symbol, config.app.interval, targetDate)
        logger.debug { "取得したK線データ件数: ${klineResponse.data.size} 件" }
        return klineResponse.data
    }

/**
     * K線データを取得するための対象日付を決定する。
     * GMOコイン等の取引所の仕様（営業日は朝6時切り替え）を考慮し、
     * 午前6時前の場合は前日の日付を返す。
     *
     * @return 対象日付の文字列（形式: yyyyMMdd）
     */
    private fun resolveKlineTargetDate(): String {
        val nowJst = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val date = if (nowJst.hour < 6) {
            nowJst.minusDays(1)
        } else {
            nowJst
        }
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

/**
     * 設定値に指定されたStrategy名から、実際に使用する売買戦略を生成する。
     *
     * 未対応のStrategy名が指定された場合は、誤った戦略で実行されないように例外を投げる。
     *
     * @param config 取引関連の設定
     * @return 使用する売買戦略
     */
    private fun createStrategy(config: TradingConfig): TradingStrategy {
        return when (config.strategyName) {
            "SafeReboundStrategy" -> SafeReboundStrategy(config)
            "CooldownReboundStrategy" -> CooldownReboundStrategy(config)
            "TrendConfirmReboundStrategy" -> TrendConfirmReboundStrategy(config)
            "AtrTrendConfirmReboundStrategy" -> AtrTrendConfirmReboundStrategy(config)
            "SimpleContrarianStrategy" -> SimpleContrarianStrategy(config)
            else -> error("Unknown strategyName: ${config.strategyName}. Supported strategies: SafeReboundStrategy, CooldownReboundStrategy, TrendConfirmReboundStrategy, AtrTrendConfirmReboundStrategy, SimpleContrarianStrategy")
        }
    }

    /**
     * シミュレーションのサマリーをログ出力する
     * @param state シミュレーション状態
     * @param currentPrice 現在の価格
     */
    private fun logSimulationSummary(
        currentState: cryptoautotrading.domain.model.SimulationState,
        nextState: cryptoautotrading.domain.model.SimulationState,
        decision: cryptoautotrading.domain.model.TradeDecision,
        currentPrice: java.math.BigDecimal,
        pnl: cryptoautotrading.domain.simulation.ProfitAndLossResult,
        estimatedHoldingValue: java.math.BigDecimal,
        totalAssetValue: java.math.BigDecimal,
        tradeAmount: Int
    ) {
        val isHoldingStr = if (nextState.isHolding) "保有中" else "なし"
        val sb = java.lang.StringBuilder()
        sb.appendLine()
        sb.appendLine("============================================================")
        sb.appendLine("【シミュレーション結果サマリー】")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("判定              : ${decision.action.description}")
        sb.appendLine("理由              : ${decision.reason}")
        sb.appendLine("現在価格          : $currentPrice")
        sb.appendLine("残金              : ${nextState.cashBalance}")
        sb.appendLine("保有BTC数量        : ${nextState.holdingAmount}")
        sb.appendLine("保有BTC評価額      : $estimatedHoldingValue")
        sb.appendLine("総資産            : $totalAssetValue")
        sb.appendLine("買値              : ${nextState.buyPrice}")
        sb.appendLine("確定損益          : ${nextState.realizedProfitAndLoss}")
        sb.appendLine("想定損益          : ${pnl.estimatedProfitAndLoss}")
        sb.appendLine("更新後の保有状態    : $isHoldingStr")

        // 買い・売りの詳細
        if (decision.action == TradeAction.BUY_CANDIDATE && !currentState.isHolding && nextState.isHolding) {
            sb.appendLine("購入金額          : $tradeAmount")
            sb.appendLine("購入BTC数量       : ${nextState.holdingAmount}")
            sb.appendLine("購入後残金        : ${nextState.cashBalance}")
        } else if (decision.action == TradeAction.SELL_CANDIDATE && currentState.isHolding && !nextState.isHolding) {
            val sellAmount = currentState.holdingAmount * currentPrice
            sb.appendLine("売却金額          : $sellAmount")
            sb.appendLine("売却BTC数量       : ${currentState.holdingAmount}")
            sb.appendLine("売却損益          : ${pnl.profitAndLoss}")
            sb.appendLine("売却後残金        : ${nextState.cashBalance}")
        }
        sb.append("============================================================")
        logger.info { sb.toString() }
    }
}
