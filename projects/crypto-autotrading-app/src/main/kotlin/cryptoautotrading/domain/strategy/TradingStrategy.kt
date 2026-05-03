package cryptoautotrading.domain.strategy

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.model.TradeDecision

/**
 * 売買判定を行う戦略インターフェース
 */
interface TradingStrategy {
    /**
     * K線データと現在のシミュレーション状態から売買判定を行う
     *
     * @param klines K線データのリスト
     * @param currentState 現在のシミュレーション状態
     * @return 判定結果
     */
    fun judge(
        klines: List<Kline>,
        currentState: SimulationState
    ): TradeDecision
}
