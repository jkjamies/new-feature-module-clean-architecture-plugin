package com.github.jkjamies.cammp.feature.usecasegenerator.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

class UseCaseGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesUseCaseInDomain() {
        val tempDir = Files.createTempDirectory("usecase-generator-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/domain layout
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "checkout")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
        }

        val generator = UseCaseGenerator(project)
        val result = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/checkout/domain",
                useCaseName = "PlaceOrderUseCase",
                diEnabled = true,
                useHilt = true,
                koinAnnotations = false
            )
        }
        assertTrue(result.contains("Use case 'PlaceOrderUseCase' generated"))

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/checkout/domain/src/main/kotlin/com/jkjamies/features/checkout/domain/usecase/PlaceOrderUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text = VfsUtilCore.loadText(useCaseFile!!)
        assertTrue(text.contains("package com.jkjamies.features.checkout.domain.usecase"))
        assertTrue(text.contains("class PlaceOrderUseCase"))
        assertTrue(text.contains("@Inject constructor"))
        assertTrue(text.contains("suspend operator fun invoke()"))
    }

    fun testGenerateWritesTodoWhenNotHilt() {
        val tempDir = Files.createTempDirectory("usecase-generator-test-nohilt").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/domain layout
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "payments")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/payments/domain",
                useCaseName = "SyncInvoicesUseCase",
                diEnabled = true,
                useHilt = false, // not Hilt
                koinAnnotations = true
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/payments/domain/src/main/kotlin/com/jkjamies/features/payments/domain/usecase/SyncInvoicesUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text = VfsUtilCore.loadText(useCaseFile!!).trim()
        assertEquals("// TODO: not yet available", text)
    }
}
