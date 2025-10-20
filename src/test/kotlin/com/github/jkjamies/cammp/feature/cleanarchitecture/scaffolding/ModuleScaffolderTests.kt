package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
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

        // create test domain module directory inside a write action
        val domainDir = WriteAction.compute<VirtualFile, RuntimeException> {
            VfsUtil.createDirectoryIfMissing(vfs, "domain") ?: error("Failed to create domain module dir")
        }

        val scaffolder = ModuleScaffolder()
        WriteAction.run<RuntimeException> {
            scaffolder.scaffoldModule(domainDir, "domain", "features", "payments", "jkjamies")
        }

        val buildFile = domainDir.findChild("build.gradle.kts") ?: error("build.gradle.kts not created")
        val buildContent = VfsUtil.loadText(buildFile)
        // Namespace should be replaced with the computed package for the domain module
        assertTrue(buildContent.contains("namespace = \"com.jkjamies.features.payments.domain\""))

        val domainPkgPath = "src/main/kotlin/com/jkjamies/features/payments/domain"
        val domainPkgDir = VfsUtil.findRelativeFile(domainPkgPath, domainDir)
            ?: error("Domain package dir not created")
        assertNotNull(domainPkgDir.findChild("Placeholder.kt"))
        // Verify clean architecture subdirectories for domain
        assertNotNull(domainPkgDir.findChild("repository"))
        assertNotNull(domainPkgDir.findChild("model"))
        assertNotNull(domainPkgDir.findChild("usecase"))

        // create test data module and scaffold
        val dataDir = WriteAction.compute<VirtualFile, RuntimeException> {
            VfsUtil.createDirectoryIfMissing(vfs, "data") ?: error("Failed to create data module dir")
        }
        WriteAction.run<RuntimeException> {
            scaffolder.scaffoldModule(dataDir, "data", "features", "payments", "jkjamies")
        }
        val dataPkgPath = "src/main/kotlin/com/jkjamies/features/payments/data"
        val dataPkgDir = VfsUtil.findRelativeFile(dataPkgPath, dataDir)
            ?: error("Data package dir not created")
        // Verify repository subdirectory for data
        assertNotNull(dataPkgDir.findChild("repository"))
    }
}
