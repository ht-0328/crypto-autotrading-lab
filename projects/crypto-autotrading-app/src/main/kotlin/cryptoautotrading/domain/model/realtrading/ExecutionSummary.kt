package cryptoautotrading.domain.model.realtrading

import java.math.BigDecimal

/**
 * 約定情報の集計結果を表すモデル。
 *
 * @property totalSize 約定数量の合計
 * @property totalCost 約定金額の合計
 * @property totalFee 取引手数料の合計
 * @property latestTimestamp 最新の約定時刻
 */
data class ExecutionSummary(
    val totalSize: BigDecimal,
    val totalCost: BigDecimal,
    val totalFee: BigDecimal,
    val latestTimestamp: String
)
