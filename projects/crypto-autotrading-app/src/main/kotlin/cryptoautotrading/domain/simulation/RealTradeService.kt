package cryptoautotrading.domain.simulation

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.model.order.OrderRequest
import cryptoautotrading.domain.port.ExchangeOrderClient
import cryptoautotrading.domain.port.SecretManager
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal

class RealTradeService(
    private val config: TradingConfig,
    private val secretManager: SecretManager,
    private val orderClient: ExchangeOrderClient
) {
    private val logger = KotlinLogging.logger {}

    suspend fun processRealOrder(state: SimulationState, currentPrice: BigDecimal) {
        if (!config.realTradeEnabled) {
            logger.info { "RealTradeService: realTradeEnabled が false のため実注文をスキップします" }
            return
        }

        if (config.dryRun) {
            logger.info { "RealTradeService: dry_run が true のため実注文をスキップし、予定のみ記録します" }
            return
        }

        try {
            logger.info { "RealTradeService: 実注文プロセスを開始します" }

            val apiKey = secretManager.getSecret(config.gmoApiKeySecretName)
            val apiSecret = secretManager.getSecret(config.gmoApiSecretSecretName)

            // 1. 残高チェック
            val assetsResponse = orderClient.getAssets(apiKey, apiSecret)
            val jpyAsset = assetsResponse.assets.find { it.symbol == "JPY" }
            val availableJpy = jpyAsset?.available ?: BigDecimal.ZERO

            val orderAmount = BigDecimal(config.tradeAmount)
            if (availableJpy < orderAmount) {
                logger.warn { "RealTradeService: JPY残高不足" }
                return
            }

            // 2. 有効注文一覧チェック (二重注文防止)
            val activeOrdersResponse = orderClient.getActiveOrders(apiKey, apiSecret, config.orderSymbol)
            if (activeOrdersResponse.orders.isNotEmpty()) {
                logger.warn { "RealTradeService: 既に有効な未約定注文が存在するため注文を見送ります。" }
                return
            }

            // 3. 上限チェック
            if (orderAmount > BigDecimal(config.maxOrderJpy)) {
                logger.warn { "RealTradeService: 注文金額が1回あたりの最大上限を超えています。" }
                return
            }

            // 4. 注文実行
            val size = (orderAmount / currentPrice).setScale(4, java.math.RoundingMode.DOWN)

            val request = OrderRequest(
                symbol = config.orderSymbol,
                side = "BUY",
                executionType = config.orderExecutionType,
                timeInForce = config.orderTimeInForce,
                price = currentPrice,
                size = size
            )

            logger.info { "RealTradeService: 注文を送信します: \$request" }
            val response = orderClient.placeOrder(apiKey, apiSecret, request)
            logger.info { "RealTradeService: 注文に成功しました。OrderID: \${response.orderId}" }

        } catch (e: Exception) {
            logger.error(e) { "RealTradeService: 注文処理中にエラーが発生しました。" }
            if (config.stopOnOrderError) {
                throw IllegalStateException("RealTradeService Error: \${e.message}", e)
            }
        }
    }
}
