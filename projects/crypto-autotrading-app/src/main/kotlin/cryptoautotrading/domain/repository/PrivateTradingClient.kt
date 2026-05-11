package cryptoautotrading.domain.repository

import java.math.BigDecimal

/**
 * 取引所のプライベートAPIへアクセスするインターフェース
 */
interface PrivateTradingClient {
    /**
     * 指定された通貨の取引余力（残高）を取得する。
     * 例: GMOコインの現物取引の場合は /private/v1/account/assets で "JPY" などを取得
     */
    suspend fun getAvailableBalance(symbol: String): BigDecimal

    /**
     * 指定された通貨ペアの未約定注文一覧を取得する。
     * 二重注文の防止などに用いる。
     */
    suspend fun getActiveOrders(symbol: String): List<ActiveOrder>

    /**
     * 新規注文を発注する。
     * @return 発注に成功した場合の注文ID
     */
    suspend fun placeOrder(
        symbol: String,
        side: OrderSide,
        executionType: String,
        timeInForce: String,
        price: BigDecimal?,
        size: BigDecimal
    ): String

    /**
     * 指定された注文IDのステータスを取得する。
     */
    suspend fun getOrderStatus(orderId: String): OrderStatusInfo
}

enum class OrderSide {
    BUY,
    SELL
}

data class ActiveOrder(
    val orderId: String,
    val symbol: String,
    val side: OrderSide,
    val size: BigDecimal,
    val price: BigDecimal?
)

data class OrderStatusInfo(
    val orderId: String,
    val status: String,
    val executedSize: BigDecimal,
    val executedPrice: BigDecimal?
)
