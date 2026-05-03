package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.repository.MarketDataClient
import cryptoautotrading.domain.repository.ResultOutputPort
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.repository.SimulationStateRepository
import cryptoautotrading.domain.repository.TradeHistoryRepository
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.strategy.SafeReboundStrategy
import cryptoautotrading.domain.strategy.SimpleContrarianStrategy
import cryptoautotrading.domain.strategy.TradingStrategy
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
    private val resultOutputPort: ResultOutputPort
) {

    private val logger = KotlinLogging.logger {}
    private val simulationService = SimulationService()

    /**
     * アプリケーションの実行を開始する
     */
    suspend fun run() {
        try {
            logger.info { "TradingApplication のメイン処理を開始します" }
            logger.debug { "実行設定: $config" }

            // 1. 状態の読み込み
            val currentState = stateRepository.load()
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
            val currentPrice = klineData.sortedBy { it.openTime }.last().close.toBigDecimal()

            // 損益と想定損益の計算
            val pnl = calculateProfitAndLoss(
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
                tradeAmount = config.trading.tradeAmount
            )
            logger.info { "Next Simulation State: $nextState" }

            // 5. 出力
            // コンソール出力
            resultOutputPort.printResult(
                price = currentPrice,
                action = decision.action,
                reason = decision.reason,
                profitAndLoss = pnl.profitAndLoss,
                estimatedProfitAndLoss = pnl.estimatedProfitAndLoss
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
                fee = fee
            )

            // 6. 状態の保存
            stateRepository.save(nextState)

            logger.info { "TradingApplication のメイン処理が正常に完了しました" }

        } catch (e: Exception) {
            logger.error(e) { "TradingApplication の実行中にエラーが発生しました: ${e.message}" }
            throw e
        }
    }

    private suspend fun fetchKlineData() = run {
        val tickerResponse = marketDataClient.getTicker(config.trading.symbol)
        val ticker = tickerResponse.data.firstOrNull()
        logger.debug { "取得したティッカー主要値: symbol=${ticker?.symbol}, last=${ticker?.last}, bid=${ticker?.bid}, ask=${ticker?.ask}" }

        val targetDate = resolveKlineTargetDate()
        val klineResponse = marketDataClient.getKlines(config.trading.symbol, config.app.interval, targetDate)
        logger.debug { "取得したK線データ件数: ${klineResponse.data.size} 件" }
        klineResponse.data
    }

    private fun resolveKlineTargetDate(): String {
        val nowJst = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val date = if (nowJst.hour < 6) {
            nowJst.minusDays(1)
        } else {
            nowJst
        }
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    private fun calculateProfitAndLoss(
        isHolding: Boolean,
        currentPrice: java.math.BigDecimal,
        buyPrice: java.math.BigDecimal,
        holdingAmount: java.math.BigDecimal,
        shouldSell: Boolean
    ): ProfitAndLossResult {
        if (!isHolding) {
            return ProfitAndLossResult()
        }

        val estimated = (currentPrice - buyPrice) * holdingAmount
        val actual = if (shouldSell) estimated else java.math.BigDecimal.ZERO
        return ProfitAndLossResult(
            profitAndLoss = actual,
            estimatedProfitAndLoss = estimated
        )
    }

    private data class ProfitAndLossResult(
        val profitAndLoss: java.math.BigDecimal = java.math.BigDecimal.ZERO,
        val estimatedProfitAndLoss: java.math.BigDecimal = java.math.BigDecimal.ZERO
    )

    private fun createStrategy(config: TradingConfig): TradingStrategy {
        return when (config.strategyName) {
            "SafeReboundStrategy" -> SafeReboundStrategy(config)
            "SimpleContrarianStrategy" -> SimpleContrarianStrategy(config)
            else -> error("Unknown strategyName: ${config.strategyName}. Supported strategies: SafeReboundStrategy, SimpleContrarianStrategy")
        }
    }
}
