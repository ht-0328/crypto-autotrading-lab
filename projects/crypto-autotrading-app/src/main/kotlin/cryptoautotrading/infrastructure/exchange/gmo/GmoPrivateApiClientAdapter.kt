package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.application.RealTradingExchangeClient
import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import java.math.BigDecimal

/**
 * [GmoPrivateApiClient] を利用して [RealTradingExchangeClient] の実装を提供するアダプタークラス
 *
 * @property gmoPrivateApiClient GMO Private API クライアント
 */
class GmoPrivateApiClientAdapter(
    private val gmoPrivateApiClient: GmoPrivateApiClient
) : RealTradingExchangeClient {

    /**
     * 資産残高を取得する。
     */
    override suspend fun getAssets(): List<ExchangeAsset> {
        return gmoPrivateApiClient.getAssets()
    }

    /**
     * 有効な（未約定の）注文一覧を取得する。
     */
    override suspend fun getActiveOrders(symbol: String): List<ExchangeActiveOrder> {
        return gmoPrivateApiClient.getActiveOrders(symbol)
    }

    /**
     * 注文を発注する。
     */
    override suspend fun placeOrder(
        symbol: String,
        side: String,
        executionType: String,
        size: BigDecimal,
        price: BigDecimal?
    ): AcceptedOrder {
        return gmoPrivateApiClient.placeOrder(
            symbol = symbol,
            side = side,
            executionType = executionType,
            size = size,
            price = price
        )
    }
}
