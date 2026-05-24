package cryptoautotrading.domain

/**
 * リアル注文前安全チェックの結果を表すデータクラス
 *
 * @property passed チェックを通過したかどうか
 * @property reason 通過しなかった場合の理由
 */
data class SafetyCheckResult(
    val passed: Boolean,
    val reason: String? = null
)
