package cryptoautotrading.domain.model

/**
 * 売買判定の結果と理由を保持するクラス
 *
 * @property action 判定されたアクション
 * @property reason 判定の理由
 */
data class TradeDecision(
    val action: TradeAction,
    val reason: String
)
