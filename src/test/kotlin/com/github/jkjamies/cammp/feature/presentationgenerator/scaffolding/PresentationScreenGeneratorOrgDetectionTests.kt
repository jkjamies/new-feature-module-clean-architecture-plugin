package com.github.jkjamies.cammp.feature.presentationgenerator.scaffolding

import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Verifies that [PresentationScreenGenerator] detects the existing organization segment
 * from sibling modules and does not fallback to the default when a package exists.
 */
class PresentationScreenGeneratorOrgDetectionTests : LightPlatformTestCase() {
    fun testDetectsOrgFromSiblingModule() {
        val tempDir = Files.createTempDirectory("presentation-generator-org-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create features/payments with sibling domain and presentation modules.
        val (featureDir, presentationDir) = WriteAction.compute<Pair<VirtualFile, VirtualFile>, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "payments")
                ?: error("Failed to create feature dir")
            val domain = VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
            val presentation = VfsUtil.createDirectoryIfMissing(feature, "presentation")
                ?: error("Failed to create presentation dir")

            // Scaffold a package inside domain to establish org segment "acme"
            val pkgDir = VfsUtil.createDirectoryIfMissing(domain, "src/main/kotlin/com/acme/features/payments/domain")
                ?: error("Failed to create package dir in domain")
            val placeholder = pkgDir.findChild("Placeholder.kt") ?: pkgDir.createChildData(this, "Placeholder.kt")
            VfsUtil.saveText(placeholder, "package com.acme.features.payments.domain\n\nobject Placeholder")
            Pair(feature, presentation)
        }

        val generator = PresentationScreenGenerator(project)
        val result = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                targetDirRelativeToProject = "features/${featureDir.name}/${presentationDir.name}",
                screenName = "Checkout",
                addNavigation = false,
                useFlowStateHolder = false,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVI,
                selectedUseCaseFqns = emptyList(),
                selectedUseCaseModulePaths = emptySet()
            )
        }
        assertTrue(result.contains("Presentation screen 'Checkout'"))

        // Expect base package directory to use "acme" org instead of fallback
        val basePkgDir = VfsUtil.findRelativeFile(
            "features/${featureDir.name}/${presentationDir.name}/src/main/kotlin/com/acme/${featureDir.parent!!.name}/${featureDir.name}/presentation",
            vfsRoot
        ) ?: error("Base package dir with acme org not created")

        // Screen directory should be lowercase of base name (no trailing "Screen" in folder)
        val screenDir = VfsUtil.findRelativeFile("checkout", basePkgDir) ?: error("screen dir not created")
        assertNotNull(screenDir.findChild("CheckoutScreen.kt"))
    }
}
