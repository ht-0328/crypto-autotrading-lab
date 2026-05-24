package cryptoautotrading.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withPrimaryConstructor
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * アーキテクチャの境界ルールを検証するテストクラス
 */
class ArchitectureTest {

    /**
     * プロジェクトの各レイヤ（domain, application, infrastructure, presentation）間の
     * 依存関係がルール通りに厳格に守られていることを検証する。
     *
     * - domain: 他のどのレイヤにも依存しない（独立していること）
     * - application: domain, infrastructure レイヤに依存する
     * - infrastructure: domain と application レイヤに依存する
     * - presentation: 全てのレイヤ（domain, application, infrastructure）に依存する（DI等のため）
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
                application.dependsOn(domain, infrastructure)
                infrastructure.dependsOn(domain, application)
                presentation.dependsOn(domain, application, infrastructure)
            }
    }

    @Test
    fun `domain の interface に infrastructure の型が漏れていないこと`() {
        Konsist.scopeFromPackage("cryptoautotrading.domain..")
            .interfaces()
            .assertTrue { koInterface ->
                // interface の関数の引数と戻り値に infrastructure パッケージの型が含まれていないかチェック
                val hasNoInfrastructureImports = koInterface.containingFile.imports.none {
                    it.name.startsWith("cryptoautotrading.infrastructure")
                }
                hasNoInfrastructureImports
            }
    }

    @Test
    fun `1つの Kotlin ファイルに data class が複数定義されていないこと`() {
        Konsist.scopeFromProject()
            .files
            .assertTrue { file ->
                val dataClasses = file.classes().filter { it.hasDataModifier }
                dataClasses.size <= 1
            }
    }

    @Test
    fun `class, data class, interface, enum class に KDoc があること`() {
        // プロダクションコードのみを対象にする (テストコードは除外)
        Konsist.scopeFromProduction()
            .classes()
            .assertTrue { it.hasKDoc }

        Konsist.scopeFromProduction()
            .interfaces()
            .assertTrue { it.hasKDoc }
    }

    @Test
    fun `すべての関数(private含む)に KDoc があること`() {
        // プロダクションコードのみを対象にする (テストコードは除外)
        Konsist.scopeFromProduction()
            .functions()
            .assertTrue { it.hasKDoc }
    }
}
