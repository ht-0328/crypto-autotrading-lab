package cryptoautotrading.presentation

import cryptoautotrading.domain.notification.NotificationMessage
import cryptoautotrading.domain.notification.NotificationSeverity
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoHttpClientFactory
import cryptoautotrading.infrastructure.notification.WebhookNotifier
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/** 通知先のURLを渡す環境変数の名前 */
private const val NOTIFICATION_WEBHOOK_URL_ENV = "NOTIFICATION_WEBHOOK_URL"

/**
 * 通知の疎通確認用メインクラス。
 *
 * 実際の売買では、通知が出るのは注文・約定・停止のときだけである。
 * つまり通常の実行では、通知が本当に届くかどうかを確かめる機会がない。
 * 実注文を始めた最初の1回が通知の初テストになる、という状況を避けるために用意している。
 */
fun main() = runBlocking {
    logger.info { "通知の疎通確認を開始します" }

    val webhookUrl = System.getenv(NOTIFICATION_WEBHOOK_URL_ENV)
    if (webhookUrl.isNullOrBlank()) {
        // URL は秘密情報なので、値ではなく環境変数名だけを示す
        logger.error { "環境変数 $NOTIFICATION_WEBHOOK_URL_ENV が設定されていません。設定して再実行してください。" }
        exitProcess(1)
    }

    val config = try {
        ConfigLoader.load()
    } catch (e: Exception) {
        logger.error(e) { "設定の読み込みに失敗しました。" }
        exitProcess(1)
    }

    if (!config.notification.enabled) {
        logger.warn {
            "notification.enabled が false ですが、疎通確認のため送信します。" +
                "本番で通知を使うには設定を true にしてください。"
        }
    }

    logger.info { "送信先のキー: ${config.notification.payloadKey}" }

    val notifier = WebhookNotifier(
        webhookUrl = webhookUrl,
        payloadKey = config.notification.payloadKey,
        httpClient = GmoHttpClientFactory.create()
    )

    notifier.notify(buildTestMessage())

    logger.info {
        "送信処理が終わりました。通知先のチャンネルにメッセージが届いているか確認してください。" +
            "届いていない場合は、URL・payload_key・チャンネルの権限を確認してください。"
    }
}

/**
 * 疎通確認で送るメッセージを組み立てる。
 *
 * 本番の通知と見分けがつくよう、確認用であることを本文に明記する。
 *
 * @return 送信するメッセージ
 */
private fun buildTestMessage(): NotificationMessage {
    return NotificationMessage(
        severity = NotificationSeverity.INFO,
        title = "通知の疎通確認",
        body = "これは疎通確認のテスト送信です。実際の売買は行われていません。\n" +
            "このメッセージが見えていれば、注文・約定・停止の通知も同じ経路で届きます。"
    )
}
