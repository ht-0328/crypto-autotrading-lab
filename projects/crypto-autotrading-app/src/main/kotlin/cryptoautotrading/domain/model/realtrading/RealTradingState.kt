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
) {

    /**
     * 指定した日付時点での、その日の累計注文額を返す。
     *
     * 記録されている日付が指定した日付と違えば、その日はまだ1件も注文していないので0を返す。
     * 日付を見ずに [dailyOrderedJpy] をそのまま使うと、前日の累計が翌日に持ち越され、
     * 上限に近づいた翌日以降は1件も注文できなくなる。
     *
     * @param date 判定したい日付（ISO形式の日付文字列）
     * @return その日の累計注文額
     */
    fun dailyOrderedJpyOn(date: String): BigDecimal {
        return if (dailyOrderedDate == date) dailyOrderedJpy else BigDecimal.ZERO
    }
}
