package cryptoautotrading.domain.model.realtrading

import cryptoautotrading.domain.model.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * リアル取引全体の状態を表すデータクラス
 *
 * @property isStopped 異常発生等により新規リアル注文を停止しているか
 * @property stopReason 停止理由
 * @property stoppedAt 停止時刻
 * @property latestOrder 最新の注文情報
 * @property dailyOrderedDate 1日の累計注文額を計算するための日付文字列（例: "2023-10-27"）
 * @property dailyOrderedJpy 1日の累計注文金額
 */
@Serializable
data class RealTradingState(
    val isStopped: Boolean = false,
    val stopReason: String? = null,
    val stoppedAt: String? = null,
    val latestOrder: RealOrderState? = null,
    val dailyOrderedDate: String? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val dailyOrderedJpy: BigDecimal = BigDecimal.ZERO
)
