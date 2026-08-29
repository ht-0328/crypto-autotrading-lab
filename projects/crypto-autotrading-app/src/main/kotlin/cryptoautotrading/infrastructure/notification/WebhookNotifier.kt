package cryptoautotrading.infrastructure.notification

import cryptoautotrading.domain.notification.NotificationMessage
import cryptoautotrading.domain.notification.Notifier
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Webhook に POST して通知を送る実装。
 *
 * Discord と Slack はどちらも「JSONの1キーに本文を入れて POST する」形式なので、
 * キー名を設定で切り替えるだけで両方に対応できる。
 *
 * @property webhookUrl 送信先のURL。**秘密情報なのでログに出さない**
 * @property payloadKey 本文を入れるJSONのキー
 * @property httpClient HTTP クライアント
 */
class WebhookNotifier(
    private val webhookUrl: String,
    private val payloadKey: String,
    private val httpClient: HttpClient
) : Notifier {

    private val logger = KotlinLogging.logger {}

    /**
     * 通知を Webhook に送る。
     *
     * 送信に失敗しても例外は投げない。通知は観測の手段であって、
     * それ自体が売買処理を止める理由にはならない。
     *
     * @param message 送る通知
     */
    override suspend fun notify(message: NotificationMessage) {
        val payload = JsonObject(mapOf(payloadKey to JsonPrimitive(formatMessage(message))))

        try {
            val response = httpClient.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(JsonObject.serializer(), payload))
            }
            // URL は秘密情報なのでログに出さない
            logger.info { "通知を送信しました。severity=${message.severity}, httpStatus=${response.status.value}" }
        } catch (e: Exception) {
            logger.warn(e) { "通知の送信に失敗しました。売買処理は継続します。severity=${message.severity}" }
        }
    }

    /**
     * 通知の本文を組み立てる。
     *
     * @param message 送る通知
     * @return 送信する文字列
     */
    private fun formatMessage(message: NotificationMessage): String {
        return "[${message.severity}] ${message.title}\n${message.body}"
    }
}
