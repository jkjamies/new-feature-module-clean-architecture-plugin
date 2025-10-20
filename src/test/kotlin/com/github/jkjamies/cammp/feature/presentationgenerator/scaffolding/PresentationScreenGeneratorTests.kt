package com.github.jkjamies.cammp.feature.presentationgenerator.scaffolding

import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Verifies [PresentationScreenGenerator] creates expected directories and files via VfsUtil.
 */
class PresentationScreenGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesScreenDirectoryAndFiles() {
        val tempDir = Files.createTempDirectory("presentation-generator-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        val presentationDir = WriteAction.compute<VirtualFile, RuntimeException> {
            // simulate feature structure: features/payments/presentation
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "payments")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "presentation")
                ?: error("Failed to create presentation dir")
        }

        val generator = PresentationScreenGenerator(project)
        val result = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                targetDirRelativeToProject = "features/payments/presentation",
                screenName = "Checkout",
                addNavigation = true,
                useFlowStateHolder = true,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVI
            )
        }
        assertTrue(result.contains("Presentation screen 'Checkout'"))

        // Base package directory: src/main/kotlin/com/jkjamies/features/payments/presentation
        val basePkgDir = VfsUtil.findRelativeFile("src/main/kotlin/com/jkjamies/features/payments/presentation", presentationDir)
            ?: error("base package dir not created")

        val screenDir = VfsUtil.findRelativeFile("checkout", basePkgDir) ?: error("screen dir not created")
        assertNotNull(screenDir.findChild("CheckoutScreen.kt"))
        assertNotNull(screenDir.findChild("CheckoutIntent.kt"))
        assertNotNull(screenDir.findChild("CheckoutViewModel.kt"))
        assertNotNull(screenDir.findChild("CheckoutUiState.kt"))
        assertNotNull(screenDir.findChild("CheckoutStateHolder.kt"))

        // Navigation artifacts
        val navDir = VfsUtil.findRelativeFile("navigation", basePkgDir) ?: error("navigation dir not created")
        assertNotNull(navDir.findChild("PaymentsNavHost.kt").also { /* uses parent module name before presentation */ })
        val destDir = VfsUtil.findRelativeFile("destinations", navDir) ?: error("destinations dir not created")
        assertNotNull(destDir.findChild("CheckoutDestination.kt"))

        // FlowStateHolder should be created inside the base package directory, with Capitalized parent name prefix
        assertNotNull(basePkgDir.findChild("PaymentsFlowStateHolder.kt"))
    }

    fun testDirectoryOmitsScreenSuffix() {
        val tempDir = Files.createTempDirectory("presentation-generator-test2").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        val presentationDir = WriteAction.compute<VirtualFile, RuntimeException> {
            // simulate feature structure: features/home/presentation
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "home")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "presentation")
                ?: error("Failed to create presentation dir")
        }

        val generator = PresentationScreenGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                targetDirRelativeToProject = "features/home/presentation",
                screenName = "HomeScreen",
                addNavigation = false,
                useFlowStateHolder = false,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVI
            )
        }

        val basePkgDir = VfsUtil.findRelativeFile("src/main/kotlin/com/jkjamies/features/home/presentation", presentationDir)
            ?: error("base package dir not created")
        val screenDir = VfsUtil.findRelativeFile("home", basePkgDir) ?: error("screen dir 'home' not created")
        // Main screen file keeps original name
        assertNotNull(screenDir.findChild("HomeScreen.kt"))
        // Ancillary files drop the trailing Screen already
        assertNotNull(screenDir.findChild("HomeIntent.kt"))
        assertNotNull(screenDir.findChild("HomeViewModel.kt"))
        assertNotNull(screenDir.findChild("HomeUiState.kt"))
        assertNotNull(screenDir.findChild("HomeScreenStateHolder.kt").let { it ?: screenDir.findChild("HomeStateHolder.kt") })
    }
}
