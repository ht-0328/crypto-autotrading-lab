package cryptoautotrading.domain.model.realtrading

/**
 * リアル注文前安全チェックの結果を表すデータクラス
 *
 * @property passed チェックを通過したかどうか
 * @property reason 通過しなかった場合の理由
 * @property shouldNotify 通過しなかったことを人に伝えるべきかどうか。
 *   保有中や未約定注文ありといった日常的な見送りまで通知すると、
 *   5分ごとに通知が届いて本当に伝えたいことが埋もれる。
 *   損失上限や連敗のように、売買が止まったことを知らせるべき場合だけ true にする
 */
data class SafetyCheckResult(
    val passed: Boolean,
    val reason: String? = null,
    val shouldNotify: Boolean = false
)
