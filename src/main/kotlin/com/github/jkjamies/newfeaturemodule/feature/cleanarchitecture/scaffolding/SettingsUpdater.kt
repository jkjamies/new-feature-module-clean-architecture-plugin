package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

/** Ensures include entries for generated modules exist in settings.gradle(.kts). */
class SettingsUpdater {
    fun updateRootSettingsIncludes(project: Project, projectBasePath: String, moduleGradlePaths: List<String>) {
        val lfs = LocalFileSystem.getInstance()
        val projectRootVf = lfs.findFileByPath(projectBasePath)
            ?: error("Project root not found: $projectBasePath")

        val settingsKts = projectRootVf.findChild("settings.gradle.kts")
        val settingsGroovy = projectRootVf.findChild("settings.gradle")
        val settingsFile = settingsKts ?: settingsGroovy
        val isKts = settingsKts != null || (settingsGroovy == null)

        val file = settingsFile ?: projectRootVf.createChildData(this, "settings.gradle.kts")
        val current = VfsUtil.loadText(file)
        val builder = StringBuilder(current)

        var modified = false
        moduleGradlePaths.forEach { path ->
            val includeLine = if (isKts) "include(\"$path\")" else "include '$path'"
            if (!current.contains(includeLine)) {
                builder.appendLine()
                builder.appendLine(includeLine)
                modified = true
            }
        }

        if (modified) {
            VfsUtil.saveText(file, builder.toString())
        }
    }
}
