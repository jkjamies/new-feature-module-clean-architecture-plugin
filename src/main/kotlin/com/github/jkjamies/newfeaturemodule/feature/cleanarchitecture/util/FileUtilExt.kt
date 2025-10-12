package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

object FileUtilExt {
    fun writeFileIfAbsent(dir: VirtualFile, name: String, content: String) {
        val existing = dir.findChild(name)
        if (existing == null) {
            val file = dir.createChildData(this, name)
            VfsUtil.saveText(file, content)
        }
    }
}
