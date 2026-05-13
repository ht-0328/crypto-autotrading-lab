package cryptoautotrading.domain.port

import cryptoautotrading.domain.model.order.ActiveOrdersResponse
import cryptoautotrading.domain.model.order.AssetsResponse
import cryptoautotrading.domain.model.order.ExecutionsResponse
import cryptoautotrading.domain.model.order.OrderRequest
import cryptoautotrading.domain.model.order.OrderResponse

interface ExchangeOrderClient {
    suspend fun getAssets(apiKey: String, apiSecret: String): AssetsResponse
    suspend fun getActiveOrders(apiKey: String, apiSecret: String, symbol: String): ActiveOrdersResponse
    suspend fun placeOrder(apiKey: String, apiSecret: String, request: OrderRequest): OrderResponse
    suspend fun getExecutions(apiKey: String, apiSecret: String, orderId: String): ExecutionsResponse
}
