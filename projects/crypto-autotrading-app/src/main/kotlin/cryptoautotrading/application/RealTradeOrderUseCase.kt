package cryptoautotrading.application

import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * リアル取引の注文処理を担当するUseCase
 */
class RealTradeOrderUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 判定結果と設定に基づいてリアル注文を実行するかどうかを判断し、処理を行う。
     *
     * 現行フェーズ(Phase 1)では実際の注文APIの呼び出しは行わず、
     * 条件に合致する場合にログ出力のみを行う。
     *
     * @param decision 戦略判定によって下された売買判定結果
     * @param config リアル取引に関する設定
     */
    fun executeOrderIfNeeded(decision: TradeDecision, config: RealTradingConfig) {
        if (config.dryRun || !config.realTradeEnabled) {
            logger.debug { "Real trading is disabled or in dry-run mode. Skipping real trade execution." }
            return
        }

        if (decision.action == TradeAction.BUY_CANDIDATE) {
            logger.info { "Real trade execution check passed for BUY_CANDIDATE. Note: Actual order API call is NOT implemented in Phase 1." }
            // 今後ここにAPIキー取得、残高確認、実際の注文API呼び出しなどを実装する
        } else {
            logger.debug { "Trade action is not BUY_CANDIDATE (${decision.action}). No real trade action taken." }
        }
    }
}
