package cryptoautotrading.domain.notification

/**
 * 通知の重大度を表す列挙型
 *
 * @property INFO 起きたことの記録。行動は求めない
 * @property WARN 気に留めるべきこと。すぐの行動までは求めない
 * @property CRITICAL 人が確認して対応する必要があること
 */
enum class NotificationSeverity {
    /** 起きたことの記録 */
    INFO,

    /** 気に留めるべきこと */
    WARN,

    /** 人が確認して対応する必要があること */
    CRITICAL
}
