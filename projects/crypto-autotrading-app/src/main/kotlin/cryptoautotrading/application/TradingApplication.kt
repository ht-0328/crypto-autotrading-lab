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
    private val pnlCalculator = ProfitAndLossCalculator()

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
            "SimpleContrarianStrategy" -> SimpleContrarianStrategy(config)
            else -> error("Unknown strategyName: ${config.strategyName}. Supported strategies: SafeReboundStrategy, SimpleContrarianStrategy")
        }
    }
}
