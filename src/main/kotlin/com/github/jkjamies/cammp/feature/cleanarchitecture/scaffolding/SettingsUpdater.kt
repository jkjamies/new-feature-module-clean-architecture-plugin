package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

/**
 * Updates the root settings file to include newly created modules.
 *
 * Supports both settings.gradle.kts and settings.gradle formats. Uses [LocalFileSystem]
 * and [VfsUtil] to read and write the file.
 */
class SettingsUpdater {
    /**
     * Ensures every path in [moduleGradlePaths] is present as an include line in the root
     * settings file located under [projectBasePath]. If no settings file exists, a
     * Kotlin DSL variant is created.
     */
    fun updateRootSettingsIncludes(projectBasePath: String, moduleGradlePaths: List<String>) {
        val lfs = LocalFileSystem.getInstance()
        val projectRootVf = lfs.findFileByPath(projectBasePath)
            // must resolve root to update settings file
            ?: error("Project root not found: $projectBasePath")

        val settingsKts = projectRootVf.findChild("settings.gradle.kts")
        val settingsGroovy = projectRootVf.findChild("settings.gradle")
        val settingsFile = settingsKts ?: settingsGroovy
        // default to Kotlin DSL when no file present
        val isKts = settingsKts != null || (settingsGroovy == null)

        // lazily create settings file
        val file = settingsFile ?: projectRootVf.createChildData(this, "settings.gradle.kts")
        val current = VfsUtil.loadText(file)
        val builder = StringBuilder(current)

        var modified = false
        moduleGradlePaths.forEach { path ->
            // construct exact include syntax per DSL
            val includeLine = if (isKts) "include(\"$path\")" else "include '$path'"
            // idempotency: append only if missing
            if (!current.contains(includeLine)) {
                builder.appendLine(includeLine)
                modified = true
            }
        }

        if (modified) {
            // write only on change to avoid unnecessary VFS events
            VfsUtil.saveText(file, builder.toString())
        }
    }

    /**
     * Ensures includeBuild for the given relative path (for example "build-logic") is present
     * in the root settings file. Creates a Kotlin DSL settings.gradle.kts if missing.
     */
    fun updateRootSettingsIncludeBuild(projectBasePath: String, buildDir: String) {
        val lfs = LocalFileSystem.getInstance()
        val projectRootVf = lfs.findFileByPath(projectBasePath)
            ?: error("Project root not found: $projectBasePath")

        val settingsKts = projectRootVf.findChild("settings.gradle.kts")
        val settingsGroovy = projectRootVf.findChild("settings.gradle")
        val settingsFile = settingsKts ?: settingsGroovy
        val isKts = settingsKts != null || (settingsGroovy == null)
        val file = settingsFile ?: projectRootVf.createChildData(this, "settings.gradle.kts")
        val current = VfsUtil.loadText(file)

        // Minimal behavior: only ensure a single includeBuild line for the provided path exists.
        val includeBuildLine = if (isKts) "includeBuild(\"$buildDir\")" else "includeBuild '$buildDir'"
        if (current.contains(includeBuildLine)) return

        val updated = StringBuilder(current)
        if (updated.isNotEmpty() && !updated.endsWith("\n")) updated.appendLine()
        updated.appendLine(includeBuildLine)
        VfsUtil.saveText(file, updated.toString())
    }
}
