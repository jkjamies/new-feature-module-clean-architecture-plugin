package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * End-to-end tests for [FeatureModulesGenerator] integrating with VFS ([LocalFileSystem], [VfsUtil]).
 */
class FeatureModulesGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesModulesAndUpdatesSettings() {
        val tempProject = Files.createTempDirectory("feature-generator-test").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        // generation touches VFS; run in write context
        val message = com.intellij.openapi.application.WriteAction.compute<String, RuntimeException> {
            generator.generate(projectRootVf.path, "features", "payments")
        }
        assertTrue(message.contains("updated settings.gradle"))

        val featureDir = VfsUtil.findRelativeFile("features/payments", projectRootVf)
            ?: error("Feature dir not created")

        listOf("domain", "data", "di", "presentation").forEach { m ->
            assertNotNull("Module $m missing", featureDir.findChild(m))
        }

        val settingsFile = projectRootVf.findChild("settings.gradle.kts")
            ?: error("settings file not created")
        val content = VfsUtil.loadText(settingsFile)
        listOf("domain", "data", "di", "presentation").forEach { m ->
            val line = "include(\":features:payments:$m\")"
            assertTrue("Missing include for $m", content.contains(line))
        }
    }
}
