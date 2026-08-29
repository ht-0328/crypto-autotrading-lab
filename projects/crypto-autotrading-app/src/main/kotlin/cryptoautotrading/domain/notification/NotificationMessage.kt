package cryptoautotrading.domain.notification

/**
 * 人に伝える1件の通知を表すデータクラス
 *
 * @property severity 通知の重大度
 * @property title 何が起きたかの見出し
 * @property body 詳細。**APIキーやシークレットなどの秘密情報を入れてはいけない**
 */
data class NotificationMessage(
    val severity: NotificationSeverity,
    val title: String,
    val body: String
)
