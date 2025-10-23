package com.github.jkjamies.cammp.feature.presentationgenerator.scaffolding

import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Verifies [PresentationScreenGenerator] Intent file is generated only for MVI pattern and omitted for MVVM.
 */
class PresentationScreenGeneratorPatternTests : LightPlatformTestCase() {
    fun testNoIntentForMvvmPattern() {
        val tempDir = Files.createTempDirectory("presentation-generator-pattern-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        val presentationDir = WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "profile")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "presentation")
                ?: error("Failed to create presentation dir")
        }

        val generator = PresentationScreenGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                targetDirRelativeToProject = "features/profile/presentation",
                screenName = "ProfileScreen",
                addNavigation = false,
                useFlowStateHolder = false,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVVM,
                selectedUseCaseFqns = emptyList(),
                selectedUseCaseModulePaths = emptySet()
            )
        }

        val basePkgDir = VfsUtil.findRelativeFile("src/main/kotlin/com/jkjamies/features/profile/presentation", presentationDir)
            ?: error("base package dir not created")
        val screenDir = VfsUtil.findRelativeFile("profile", basePkgDir) ?: error("screen dir 'profile' not created")
        assertNotNull(screenDir.findChild("ProfileScreen.kt"))
        // Intent should NOT be created for MVVM
        assertNull(screenDir.findChild("ProfileIntent.kt"))
        // Other supporting files should still be present
        assertNotNull(screenDir.findChild("ProfileViewModel.kt"))
        assertNotNull(screenDir.findChild("ProfileUiState.kt"))
    }
}
