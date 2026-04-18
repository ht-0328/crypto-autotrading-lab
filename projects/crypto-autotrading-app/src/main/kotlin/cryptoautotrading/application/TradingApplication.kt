package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.strategy.TradingStrategy
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.output.StateRepository
import io.github.oshai.kotlinlogging.KotlinLogging

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
    private val stateRepository = StateRepository(config.output.statePath)
    private val simulationService = SimulationService()

    /**
     * アプリケーションの実行を開始する
     */
    suspend fun run() {
        try {
            logger.info { "Running trading application with config: $config" }

            // 1. 状態の読み込み
            val currentState = stateRepository.load()
            logger.info { "Current Simulation State: $currentState" }

            // 2. APIからデータの取得
            val tickerResponse = apiClient.getTicker(config.trading.symbol)
            logger.info { "Ticker Response: $tickerResponse" }

            val klineResponse = apiClient.getKlines(config.trading.symbol, config.app.interval, "20231001")
            logger.info { "Klines Response: $klineResponse" }

            // 3. 売買判定
            val strategy = TradingStrategy()
            val decision = strategy.judge(klineResponse.data, currentState.isHolding)
            logger.info { "Trade Decision: ${decision.action.description}, Reason: ${decision.reason}" }

            // 4. 状態の更新
            // 最新のK線の終値を現在価格とする
            val currentPrice = klineResponse.data.sortedBy { it.openTime }.last().close.toDouble()
            val nextState = simulationService.updateState(
                currentState = currentState,
                decision = decision,
                currentPrice = currentPrice,
                tradeAmount = config.trading.tradeAmount
            )
            logger.info { "Next Simulation State: $nextState" }

            // 5. 状態の保存
            stateRepository.save(nextState)

        } catch (e: Exception) {
            logger.error(e) { "Failed to run trading application" }
        }
    }
}
