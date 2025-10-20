package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Tests that [SettingsUpdater] appends include lines only once in settings.gradle(.kts) and settings.gradle.
 */
class SettingsUpdaterTests : LightPlatformTestCase() {
    fun testUpdateRootSettingsIncludesAppendsOnceKts() {
        val tempRoot = Files.createTempDirectory("settings-updater-test").toFile()
        val vfRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempRoot)
            ?: error("root VFS not found")

        val settingsFile = WriteAction.compute<VirtualFile, RuntimeException> {
            val f = vfRoot.createChildData(this, "settings.gradle.kts")
            VfsUtil.saveText(f, "rootProject.name = \"demo\"\n")
            f
        }

        val updater = SettingsUpdater()
        val paths = listOf(":features:payments:domain", ":features:payments:data")

        // perform updates twice to verify idempotency
        WriteAction.run<RuntimeException> {
            updater.updateRootSettingsIncludes(vfRoot.path, paths)
            updater.updateRootSettingsIncludes(vfRoot.path, paths)
        }

        val content = VfsUtil.loadText(settingsFile)
        val domainLine = "include(\":features:payments:domain\")"
        val dataLine = "include(\":features:payments:data\")"
        // ensure exactly one include line exists
        assertEquals(1, Regex("^${Regex.escape(domainLine)}$", RegexOption.MULTILINE).findAll(content).count())
        // ensure exactly one include line exists
        assertEquals(1, Regex("^${Regex.escape(dataLine)}$", RegexOption.MULTILINE).findAll(content).count())
    }

    fun testUpdateRootSettingsIncludesSupportsGroovy() {
        val tempRoot = Files.createTempDirectory("settings-updater-test-groovy").toFile()
        val vfRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempRoot)
            ?: error("root VFS not found")

        val settingsFile = WriteAction.compute<VirtualFile, RuntimeException> {
            val f = vfRoot.createChildData(this, "settings.gradle")
            VfsUtil.saveText(f, "rootProject.name = 'demo'\n")
            f
        }

        val updater = SettingsUpdater()
        val paths = listOf(":features:catalog:domain", ":features:catalog:data")

        WriteAction.run<RuntimeException> {
            updater.updateRootSettingsIncludes(vfRoot.path, paths)
            updater.updateRootSettingsIncludes(vfRoot.path, paths)
        }

        val content = VfsUtil.loadText(settingsFile)
        val domainLine = "include ':features:catalog:domain'"
        val dataLine = "include ':features:catalog:data'"
        assertEquals(1, Regex("^${Regex.escape(domainLine)}$", RegexOption.MULTILINE).findAll(content).count())
        assertEquals(1, Regex("^${Regex.escape(dataLine)}$", RegexOption.MULTILINE).findAll(content).count())
    }
}
