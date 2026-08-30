package cryptoautotrading.domain.notification

/**
 * 人に通知を届ける口。
 *
 * 手動の承認を置かない運転では、発注の直前に人が内容を見る機会がない。
 * 起きたことを事後に必ず知ることだけが、人間側の歯止めになる。
 *
 * 実装は infrastructure に置く。domain からは外部への送信手段を知らない。
 */
interface Notifier {

    /**
     * 通知を送る。
     *
     * **送信に失敗しても例外を投げてはいけない。** 通知は観測の手段であって、
     * それ自体が売買処理を止める理由にはならない。
     *
     * ただし、送れたかどうかは呼び出し側に返す。1日1回だけ送る通知は、
     * 送れていないのに「送った」と記録すると、その日の分が永久に失われる。
     *
     * @param message 送る通知
     * @return 実際に送信できた場合は true
     */
    suspend fun notify(message: NotificationMessage): Boolean
}
