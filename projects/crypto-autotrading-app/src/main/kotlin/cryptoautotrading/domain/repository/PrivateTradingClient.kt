package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.order.ActiveOrder
import cryptoautotrading.domain.model.order.OrderSide
import cryptoautotrading.domain.model.order.OrderStatusInfo
import java.math.BigDecimal

/**
 * 取引所ごとのプライベートAPI（認証が必要なAPI）を呼び出すクライアントのインターフェース
 */
interface PrivateTradingClient {
    /**
     * 現在の資産情報を取得します。
     *
     * @return 資産情報のリスト
     */
    suspend fun getAssets(): List<cryptoautotrading.infrastructure.exchange.gmo.model.GmoAsset>

    /**
     * 有効な注文一覧を取得します。
     *
     * @param symbol 対象のシンボル
     * @return 有効な注文のリスト
     */
    suspend fun getActiveOrders(symbol: String): List<ActiveOrder>

    /**
     * 新規注文を発注します。
     *
     * @param symbol シンボル
     * @param side 売買区分
     * @param executionType 執行数量条件
     * @param timeInForce 有効期間条件
     * @param price 注文価格（成行の場合はnull）
     * @param size 注文数量
     * @return 注文ID
     */
    suspend fun order(
        symbol: String,
        side: OrderSide,
        executionType: String,
        timeInForce: String,
        price: BigDecimal?,
        size: BigDecimal
    ): Long

    /**
     * 指定した注文IDのステータスを取得します。
     *
     * @param orderId 注文ID
     * @return 注文ステータス情報。存在しない場合はnull
     */
    suspend fun getOrders(orderId: Long): OrderStatusInfo?
}
