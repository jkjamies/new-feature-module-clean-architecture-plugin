package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Verifies [ModuleScaffolder] creates expected directories and files via VfsUtil.
 */
class ModuleScaffolderTests : LightPlatformTestCase() {
    fun testScaffoldCreatesExpectedStructure() {
        val tempDir = Files.createTempDirectory("module-scaffolder-test").toFile()
        val vfs = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // create test module directory inside a write action
        val moduleDir = com.intellij.openapi.application.WriteAction.compute<com.intellij.openapi.vfs.VirtualFile, RuntimeException> {
            VfsUtil.createDirectoryIfMissing(vfs, "domain") ?: error("Failed to create module dir")
        }

        val scaffolder = ModuleScaffolder()
        com.intellij.openapi.application.WriteAction.run<RuntimeException> {
            scaffolder.scaffoldModule(moduleDir, "domain", "features", "payments", "jkjamies")
        }

        val buildFile = moduleDir.findChild("build.gradle.kts") ?: error("build.gradle.kts not created")
        val buildContent = VfsUtil.loadText(buildFile)
        // Namespace should be replaced with the computed package for the domain module
        assertTrue(buildContent.contains("namespace = \"com.jkjamies.features.payments.domain\""))

        val pkgPath = "src/main/kotlin/com/jkjamies/features/payments/domain"
        val pkgDir = VfsUtil.findRelativeFile(pkgPath, moduleDir)
            ?: error("Package dir not created")
        assertNotNull(pkgDir.findChild("Placeholder.kt"))
    }
}
