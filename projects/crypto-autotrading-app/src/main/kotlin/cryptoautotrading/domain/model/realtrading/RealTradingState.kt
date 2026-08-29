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
 * @property dailyResultDate 1日の損益と連敗の集計対象になっている日付（例: "2023-10-27"）
 * @property dailyRealizedProfitAndLoss その日に確定した損益の合計
 * @property consecutiveLossCount 連続して損失で終わった回数
 */
@Serializable
data class RealTradingState(
    val isStopped: Boolean = false,
    val stopReason: String? = null,
    val stoppedAt: String? = null,
    val latestOrder: RealOrderState? = null,
    val dailyOrderedDate: String? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val dailyOrderedJpy: BigDecimal = BigDecimal.ZERO,

    val dailyResultDate: String? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val dailyRealizedProfitAndLoss: BigDecimal = BigDecimal.ZERO,

    val consecutiveLossCount: Int = 0
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

    /**
     * 指定した日付時点での、その日の確定損益を返す。
     *
     * 記録されている日付が違えば、その日はまだ1件も確定していないので0を返す。
     *
     * @param date 判定したい日付（ISO形式の日付文字列）
     * @return その日の確定損益
     */
    fun dailyRealizedProfitAndLossOn(date: String): BigDecimal {
        return if (dailyResultDate == date) dailyRealizedProfitAndLoss else BigDecimal.ZERO
    }

    /**
     * 指定した日付時点での連敗回数を返す。
     *
     * 連敗による停止は、日付が変われば解除される。解除されないと、停止したまま
     * 売買が行われず、連敗が途切れる機会も無くなって永久に止まったままになる。
     *
     * @param date 判定したい日付（ISO形式の日付文字列）
     * @return その日時点の連敗回数
     */
    fun consecutiveLossCountOn(date: String): Int {
        return if (dailyResultDate == date) consecutiveLossCount else 0
    }

    /**
     * 確定した損益を、その日の集計に反映した状態を返す。
     *
     * 日付が変わっていれば、その日の集計は0から数え直す。
     *
     * @param date 確定した日付（ISO形式の日付文字列）
     * @param profitAndLoss 確定した損益
     * @return 集計を反映した状態
     */
    fun withRealizedResult(date: String, profitAndLoss: BigDecimal): RealTradingState {
        val isLoss = profitAndLoss < BigDecimal.ZERO
        val previousLossCount = consecutiveLossCountOn(date)

        return copy(
            dailyResultDate = date,
            dailyRealizedProfitAndLoss = dailyRealizedProfitAndLossOn(date).add(profitAndLoss),
            consecutiveLossCount = if (isLoss) previousLossCount + 1 else 0
        )
    }
}
