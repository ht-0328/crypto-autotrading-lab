package cryptoautotrading.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

class ArchitectureTest {

    @Test
    fun `layer dependencies are strictly respected`() {
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
