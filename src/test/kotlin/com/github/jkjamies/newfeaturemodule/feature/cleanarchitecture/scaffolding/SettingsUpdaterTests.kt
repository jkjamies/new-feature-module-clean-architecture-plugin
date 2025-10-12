package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Tests that [SettingsUpdater] appends include lines only once in settings.gradle(.kts).
 */
class SettingsUpdaterTests : LightPlatformTestCase() {
    fun testUpdateRootSettingsIncludesAppendsOnce() {
        val tempRoot = Files.createTempDirectory("settings-updater-test").toFile()
        val vfRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempRoot)
            ?: error("root VFS not found")

        val settingsFile = com.intellij.openapi.application.WriteAction.compute<VirtualFile, RuntimeException> {
            val f = vfRoot.createChildData(this, "settings.gradle.kts")
            VfsUtil.saveText(f, "rootProject.name = \"demo\"\n")
            f
        }

        val updater = SettingsUpdater()
        val paths = listOf(":features:payments:domain", ":features:payments:data")

        // perform updates twice to verify idempotency
        com.intellij.openapi.application.WriteAction.run<RuntimeException> {
            updater.updateRootSettingsIncludes(project, vfRoot.path, paths)
            updater.updateRootSettingsIncludes(project, vfRoot.path, paths)
        }

        val content = VfsUtil.loadText(settingsFile)
        val domainLine = "include(\":features:payments:domain\")"
        val dataLine = "include(\":features:payments:data\")"
        // ensure exactly one include line exists
        assertEquals(1, Regex("^${Regex.escape(domainLine)}$", RegexOption.MULTILINE).findAll(content).count())
        // ensure exactly one include line exists
        assertEquals(1, Regex("^${Regex.escape(dataLine)}$", RegexOption.MULTILINE).findAll(content).count())
    }
}
