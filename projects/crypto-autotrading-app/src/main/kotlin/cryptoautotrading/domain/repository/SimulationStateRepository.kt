package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.SimulationState

/**
 * シミュレーション状態を保存・読み込みするインターフェース
 */
interface SimulationStateRepository {
    /**
     * シミュレーション状態を読み込む
     */
    fun load(): SimulationState

    /**
     * シミュレーション状態を保存する
     */
    fun save(state: SimulationState)
}
