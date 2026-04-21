package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.strategy.TradingStrategy
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.output.ConsoleOutput
import cryptoautotrading.infrastructure.output.CsvRepository
import cryptoautotrading.infrastructure.output.StateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * トレーディングアプリケーションのメインロジックを実行するクラス
 *
 * @property config アプリケーション設定
 * @property apiClient GMOパブリックAPIクライアント
 */
class TradingApplication(
    private val config: AppConfig,
    private val apiClient: GmoPublicApiClient
) {

    private val logger = KotlinLogging.logger {}
    private val stateRepository: StateRepository
    private val csvRepository: CsvRepository
    private val simulationService = SimulationService()

    init {
        val dataDirEnv = System.getenv("APP_DATA_DIR")
        if (dataDirEnv.isNullOrBlank()) {
            throw IllegalStateException("APP_DATA_DIR is not set")
        }

        val statePath = java.nio.file.Paths.get(dataDirEnv, config.output.statePath).toString()
        val csvPath = java.nio.file.Paths.get(dataDirEnv, config.output.outputPath).toString()

        stateRepository = StateRepository(statePath)
        csvRepository = CsvRepository(csvPath)
    }

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
            val tickerResponse = apiClient.getTicker(config.trading.symbol)
            val ticker = tickerResponse.data.firstOrNull()
            logger.debug { "取得したティッカー主要値: symbol=${ticker?.symbol}, last=${ticker?.last}, bid=${ticker?.bid}, ask=${ticker?.ask}" }

            val nowJst = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
            val targetDate = if (nowJst.hour < 6) {
                nowJst.minusDays(1)
            } else {
                nowJst
            }.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val klineResponse = apiClient.getKlines(config.trading.symbol, config.app.interval, targetDate)
            logger.debug { "取得したK線データ件数: ${klineResponse.data.size} 件" }

            // 3. 売買判定
            val strategy = TradingStrategy(config.trading)
            val decision = strategy.judge(klineResponse.data, currentState.isHolding)
            logger.info { "Trade Decision: ${decision.action.description}, Reason: ${decision.reason}" }

            // 4. 状態の更新
            // 最新のK線の終値を現在価格とする。データが空の場合は終了する
            if (klineResponse.data.isEmpty()) {
                logger.warn { "Klines data is empty. Skipping this run." }
                return
            }
            val currentPrice = klineResponse.data.sortedBy { it.openTime }.last().close.toDouble()

            // 損益と想定損益の計算
            var profitAndLoss = 0.0
            var estimatedProfitAndLoss = 0.0
            val fee = 0.0 // Phase1 では手数料ゼロとする

            if (currentState.isHolding) {
                // 保有中の場合は現在価格との差分で想定損益を計算
                estimatedProfitAndLoss = (currentPrice - currentState.buyPrice) * currentState.holdingAmount

                // 売却する場合は実際の損益となる
                if (decision.action == TradeAction.SELL_CANDIDATE) {
                    profitAndLoss = estimatedProfitAndLoss
                }
            }

            val nextState = simulationService.updateState(
                currentState = currentState,
                decision = decision,
                currentPrice = currentPrice,
                tradeAmount = config.trading.tradeAmount
            )
            logger.info { "Next Simulation State: $nextState" }

            // 5. 出力
            // コンソール出力
            ConsoleOutput.printResult(
                price = currentPrice,
                action = decision.action,
                reason = decision.reason,
                profitAndLoss = profitAndLoss,
                estimatedProfitAndLoss = estimatedProfitAndLoss
            )

            // CSV出力
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            csvRepository.append(
                datetime = nowStr,
                price = currentPrice,
                sign = decision.action.description,
                reason = decision.reason,
                profitAndLoss = profitAndLoss,
                isHolding = nextState.isHolding,
                fee = fee
            )

            // 6. 状態の保存
            stateRepository.save(nextState)

            logger.info { "TradingApplication のメイン処理が正常に完了しました" }

        } catch (e: Exception) {
            logger.error(e) { "TradingApplication の実行中にエラーが発生しました: ${e.message}" }
        }
    }
}
