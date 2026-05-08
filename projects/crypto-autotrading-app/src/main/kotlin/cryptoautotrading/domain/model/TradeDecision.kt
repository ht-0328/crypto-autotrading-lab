package cryptoautotrading.domain.model

import java.math.BigDecimal

/**
 * 売買判定の結果と理由を保持するクラス
 *
 * @property action 判定されたアクション
 * @property reason 判定の理由
 * @property atr エントリー時に算出されたATR。AtrTrendConfirmReboundStrategyで使用
 */
data class TradeDecision(
    val action: TradeAction,
    val reason: String,
    val atr: BigDecimal? = null
)
