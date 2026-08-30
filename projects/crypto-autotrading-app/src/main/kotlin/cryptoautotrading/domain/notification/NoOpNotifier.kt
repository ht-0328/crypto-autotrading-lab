package cryptoautotrading.domain.notification

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 通知を送らない実装。
 *
 * 通知が無効な場合や、通知先を用意していないテストで使う。
 * 送るはずだった内容はログに残し、通知が設定されていれば何が届いたかを追えるようにする。
 */
object NoOpNotifier : Notifier {

    private val logger = KotlinLogging.logger {}

    /**
     * 通知を送らず、内容をログに残す。
     *
     * 送っていないので常に false を返す。これにより、通知が無効な日に
     * 有効化した場合でも、その日のうちに送られる。
     *
     * @param message 送るはずだった通知
     * @return 常に false
     */
    override suspend fun notify(message: NotificationMessage): Boolean {
        logger.debug { "通知は無効です。送信内容: [${message.severity}] ${message.title} / ${message.body}" }
        return false
    }
}
