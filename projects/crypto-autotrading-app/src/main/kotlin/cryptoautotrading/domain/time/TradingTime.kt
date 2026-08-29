package cryptoautotrading.domain.time

import java.time.Clock
import java.time.ZoneId

/**
 * 売買で時刻を扱うときの基準。
 *
 * GMOコインは日本の取引所で、営業日の切り替えも日次の集計も日本時間が基準になる。
 * 実行環境のタイムゾーン設定に左右されないよう、基準を1箇所に固定する。
 *
 * 現在時刻そのものは [Clock] として外から注入する。domain の中で直接取得すると、
 * 日付境界の挙動を固定した時刻で検証できなくなるためである。
 */
object TradingTime {

    /** 取引の基準となるタイムゾーン（日本時間） */
    val ZONE: ZoneId = ZoneId.of("Asia/Tokyo")

    /**
     * 実時刻を返す既定の時計を返す。
     *
     * テストでは [Clock.fixed] などに差し替えて、日付境界を固定した時刻で検証する。
     *
     * @return 日本時間の実時刻を返す時計
     */
    fun systemClock(): Clock = Clock.system(ZONE)
}
