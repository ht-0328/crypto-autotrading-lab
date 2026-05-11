package cryptoautotrading.infrastructure.exchange.gmo

import kotlinx.serialization.Serializable

@Serializable
data class GmoAssetsResponse(
    val status: Int,
    val data: List<GmoAsset>,
    val responsetime: String
)

@Serializable
data class GmoAsset(
    val amount: String,
    val available: String,
    val conversionRate: String,
    val symbol: String
)

@Serializable
data class GmoActiveOrdersResponse(
    val status: Int,
    val data: GmoActiveOrdersData?,
    val responsetime: String
)

@Serializable
data class GmoActiveOrdersData(
    val pagination: GmoPagination,
    val list: List<GmoOrder>
)

@Serializable
data class GmoPagination(
    val currentPage: Int,
    val count: Int
)

@Serializable
data class GmoOrder(
    val rootOrderId: Long,
    val orderId: Long,
    val symbol: String,
    val side: String,
    val orderType: String,
    val executionType: String,
    val settleType: String,
    val size: String,
    val executedSize: String,
    val price: String,
    val losscutPrice: String,
    val status: String,
    val timeInForce: String,
    val timestamp: String
)

@Serializable
data class GmoOrderRequest(
    val symbol: String,
    val side: String,
    val executionType: String,
    val timeInForce: String? = null,
    val price: String? = null,
    val size: String
)

@Serializable
data class GmoOrderResponse(
    val status: Int,
    val data: String, // 注文ID
    val responsetime: String
)

@Serializable
data class GmoOrdersResponse(
    val status: Int,
    val data: GmoOrdersData?,
    val responsetime: String
)

@Serializable
data class GmoOrdersData(
    val list: List<GmoOrder>
)
