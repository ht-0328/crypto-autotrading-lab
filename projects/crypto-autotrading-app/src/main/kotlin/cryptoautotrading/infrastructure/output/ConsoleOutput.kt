package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.repository.ResultOutputPort

object ConsoleOutput : ResultOutputPort {

    /**
     * コンソールに出力する
     */
    override fun printResult(
        price: Double,
        action: TradeAction,
        reason: String,
        profitAndLoss: Double,
        estimatedProfitAndLoss: Double
    ) {
        println("--- 判定結果 ---")
        println("現在価格: $price")

        // サイン時（買い候補、売り候補）だけ詳しく表示するという仕様があるため、
        // 見送り・保有中は簡略化する
        if (action == TradeAction.BUY_CANDIDATE || action == TradeAction.SELL_CANDIDATE) {
            println("売買サイン: ${action.description}")
            println("理由: $reason")
            println("損益: $profitAndLoss")
            println("想定損益: $estimatedProfitAndLoss")
        } else {
            println("売買サイン: ${action.description}")
            println("損益: $profitAndLoss")
        }
        println("----------------")
    }
}
