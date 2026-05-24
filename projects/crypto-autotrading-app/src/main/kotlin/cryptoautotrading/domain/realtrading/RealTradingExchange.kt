package cryptoautotrading.domain.realtrading

import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.order.ExchangeOrderStatus
import cryptoautotrading.domain.model.order.ExecutedOrder
import java.math.BigDecimal

/**
 * リアル取引の取引所操作インターフェース
 */
interface RealTradingExchange {
    /**
     * 資産残高を取得する。
     *
     * @return 資産残高のリスト
     */
    suspend fun getAssets(): List<ExchangeAsset>

    /**
     * 有効な（未約定の）注文一覧を取得する。
     *
     * @param symbol 銘柄名
     * @return 未約定注文のリスト
     */
    suspend fun getActiveOrders(symbol: String): List<ExchangeActiveOrder>

    /**
     * 注文を発注する。
     *
     * @param symbol 銘柄名
     * @param side 売買区分 (BUY, SELL)
     * @param executionType 注文タイプ (MARKET, LIMIT等)
     * @param size 注文数量
     * @param price 注文価格（成行の場合はnull）
     * @return 受け付けられた注文情報
     */
    suspend fun placeOrder(
        symbol: String,
        side: String,
        executionType: String,
        size: BigDecimal,
        price: BigDecimal? = null
    ): AcceptedOrder

    /**
     * 注文の詳細ステータスを取得する。
     *
     * @param orderId 注文ID
     * @return 注文ステータスのリスト
     */
    suspend fun getOrders(orderId: String): List<ExchangeOrderStatus>

    /**
     * 約定履歴を取得する。
     *
     * @param orderId 注文ID
     * @return 約定履歴のリスト
     */
    suspend fun getExecutions(orderId: String): List<ExecutedOrder>
}
