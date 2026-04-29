package cryptoautotrading.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

/**
 * アーキテクチャの境界ルールを検証するテストクラス
 */
class ArchitectureTest {

    /**
     * プロジェクトの各レイヤ（domain, application, infrastructure, presentation）間の
     * 依存関係がルール通りに厳格に守られていることを検証する。
     */
    @Test
    fun `各レイヤ間の依存関係がルール通りに厳格に守られていること`() {
        // Arrange
        val domain = Layer("domain", "cryptoautotrading.domain..")
        val application = Layer("application", "cryptoautotrading.application..")
        val infrastructure = Layer("infrastructure", "cryptoautotrading.infrastructure..")
        val presentation = Layer("presentation", "cryptoautotrading.presentation..")

        // Act & Assert
        Konsist.scopeFromProject()
            .assertArchitecture {
                domain.dependsOnNothing()
                application.dependsOn(domain)
                infrastructure.dependsOn(domain, application)
                presentation.dependsOn(domain, application, infrastructure)
            }
    }
}
