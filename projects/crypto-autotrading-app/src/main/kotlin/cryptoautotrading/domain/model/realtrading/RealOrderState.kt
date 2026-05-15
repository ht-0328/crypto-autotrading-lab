package cryptoautotrading.domain.model.realtrading

import cryptoautotrading.domain.model.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * リアル注文個別の状態を表すデータクラス
 *
 * @property orderId 注文ID
 * @property symbol 取引銘柄
 * @property side 売買方向
 * @property status 注文ステータス
 * @property requestedAmountJpy 注文予定金額(JPY)
 * @property requestedSize 注文予定数量
 * @property requestedPrice 注文時価格
 * @property executedPrice 約定価格
 * @property executedSize 約定数量
 * @property orderedAt 注文時刻
 * @property executedAt 約定時刻
 */
@Serializable
data class RealOrderState(
    val orderId: String,
    val symbol: String,
    val side: RealOrderSide,
    val status: RealOrderStatus,

    @Serializable(with = BigDecimalSerializer::class)
    val requestedAmountJpy: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val requestedSize: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val requestedPrice: BigDecimal? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val executedPrice: BigDecimal? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val executedSize: BigDecimal? = null,

    val orderedAt: String? = null,
    val executedAt: String? = null
)
