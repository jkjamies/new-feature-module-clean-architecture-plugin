package com.github.jkjamies.cammp.feature.cleanarchitecture.util

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Tests for [FileUtilExt] focused on non-overwriting behavior of [FileUtilExt.writeFileIfAbsent].
 */
class FileUtilExtTests : LightPlatformTestCase() {
    fun testWriteFileIfAbsentDoesNotOverwrite() {
        val tempDir = Files.createTempDirectory("fileutilext-test").toFile()
        val vfs = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        val name = "sample.txt"
        // VFS mutations must occur inside a write action
        com.intellij.openapi.application.WriteAction.run<RuntimeException> {
            FileUtilExt.writeFileIfAbsent(vfs, name, "first")
            // second call should be a no-op to preserve original content
            FileUtilExt.writeFileIfAbsent(vfs, name, "second")
        }

        val file = vfs.findChild(name) ?: error("File not created")
        val text = VfsUtil.loadText(file)
        assertEquals("first", text)
    }
}
