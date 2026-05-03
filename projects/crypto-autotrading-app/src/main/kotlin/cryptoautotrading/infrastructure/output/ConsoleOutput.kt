package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.repository.ResultOutputPort
import java.math.BigDecimal

object ConsoleOutput : ResultOutputPort {

    /**
     * 実行結果をコンソールに標準出力する。
     * 売買のアクションが発生した場合のみ、詳細な理由と想定損益を出力し、
     * 見送りなどの場合は簡略化して出力する。
     *
     * @param price 現在の価格
     * @param action 判定された売買アクション
     * @param reason アクションを決定した理由
     * @param profitAndLoss 確定した損益
     * @param estimatedProfitAndLoss 現在の含み損益（想定損益）
     */
    override fun printResult(
        price: BigDecimal,
        action: TradeAction,
        reason: String,
        profitAndLoss: BigDecimal,
        estimatedProfitAndLoss: BigDecimal,
        cashBalance: BigDecimal,
        holdingAmount: BigDecimal,
        buyPrice: BigDecimal,
        realizedProfitAndLoss: BigDecimal
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
            println("残金: $cashBalance")
            println("保有BTC数量: $holdingAmount")
            println("買値: $buyPrice")
            println("確定損益: $realizedProfitAndLoss")
        } else {
            println("売買サイン: ${action.description}")
            println("損益: $profitAndLoss")
            println("残金: $cashBalance")
            println("保有BTC数量: $holdingAmount")
            println("買値: $buyPrice")
            println("確定損益: $realizedProfitAndLoss")
        }
        println("----------------")
    }
}
