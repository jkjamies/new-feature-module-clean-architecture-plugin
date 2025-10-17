package com.github.jkjamies.cammp.feature.cleanarchitecture.util

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

/**
 * Small VFS helpers for file creation and writing.
 */
object FileUtilExt {
    /**
     * Writes [content] into a child file named [name] under [dir] if it does not already exist.
     * Uses [VfsUtil.saveText] to persist content into the [VirtualFile].
     */
    fun writeFileIfAbsent(dir: VirtualFile, name: String, content: String) {
        val existing = dir.findChild(name)
        // create-once semantics: do not overwrite an existing file
        if (existing == null) {
            val file = dir.createChildData(this, name)
            VfsUtil.saveText(file, content)
        }
    }
}
