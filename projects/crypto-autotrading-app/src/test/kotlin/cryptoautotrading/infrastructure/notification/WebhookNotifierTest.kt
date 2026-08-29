package cryptoautotrading.infrastructure.notification

import cryptoautotrading.domain.notification.NotificationMessage
import cryptoautotrading.domain.notification.NotificationSeverity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebhookNotifierTest {

    /** 秘密情報が混ざっていないかを確かめるため、URLに分かりやすい印を入れておく */
    private val webhookUrl = "https://example.com/webhook/secret-token"

    private val capturedRequests = mutableListOf<HttpRequestData>()

    private fun mockClient(status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedRequests.add(request)
                    if (status.value >= 400) {
                        respondError(status)
                    } else {
                        respond("", status, headersOf())
                    }
                }
            }
        }
    }

    private fun requestBody(request: HttpRequestData): String = (request.body as TextContent).text

    @Test
    fun `設定したキーに本文が入って送信されること`() = runTest {
        val notifier = WebhookNotifier(webhookUrl, "content", mockClient())

        notifier.notify(
            NotificationMessage(NotificationSeverity.INFO, "実注文を送信しました", "銘柄: BTC")
        )

        assertEquals(1, capturedRequests.size)
        val body = requestBody(capturedRequests.first())
        assertTrue(body.contains("\"content\""), "設定したキーが使われること: $body")
        assertTrue(body.contains("実注文を送信しました"), "見出しが含まれること: $body")
        assertTrue(body.contains("銘柄: BTC"), "本文が含まれること: $body")
    }

    @Test
    fun `Slack向けのキーにも切り替えられること`() = runTest {
        val notifier = WebhookNotifier(webhookUrl, "text", mockClient())

        notifier.notify(NotificationMessage(NotificationSeverity.INFO, "見出し", "本文"))

        val body = requestBody(capturedRequests.first())
        assertTrue(body.contains("\"text\""), "設定したキーが使われること: $body")
    }

    @Test
    fun `重大度が本文に含まれること`() = runTest {
        val notifier = WebhookNotifier(webhookUrl, "content", mockClient())

        notifier.notify(NotificationMessage(NotificationSeverity.CRITICAL, "停止しました", "理由"))

        val body = requestBody(capturedRequests.first())
        assertTrue(body.contains("CRITICAL"), "重大度が含まれること: $body")
    }

    @Test
    fun `送信先のURLが本文に含まれないこと`() = runTest {
        val notifier = WebhookNotifier(webhookUrl, "content", mockClient())

        notifier.notify(NotificationMessage(NotificationSeverity.INFO, "見出し", "本文"))

        // URL は秘密情報なので、送信する内容に混ざってはいけない
        val body = requestBody(capturedRequests.first())
        assertFalse(body.contains("secret-token"), "URL が本文に混ざっています: $body")
    }

    @Test
    fun `送信に失敗しても例外を投げないこと`() = runTest {
        val notifier = WebhookNotifier(webhookUrl, "content", mockClient(HttpStatusCode.InternalServerError))

        // 通知は観測の手段であって、それ自体が売買処理を止める理由にはならない
        notifier.notify(NotificationMessage(NotificationSeverity.INFO, "見出し", "本文"))
    }
}
