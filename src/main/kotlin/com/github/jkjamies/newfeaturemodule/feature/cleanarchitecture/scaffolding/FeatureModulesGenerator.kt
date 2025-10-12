package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util.GradlePathUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

/** Coordinates generating the feature modules and updating settings.gradle. */
class FeatureModulesGenerator(private val project: Project) {
    private val moduleScaffolder = ModuleScaffolder()
    private val settingsUpdater = SettingsUpdater()

    fun generate(projectBasePath: String, rootName: String, featureName: String): String {
        val lfs = LocalFileSystem.getInstance()
        val baseDir = lfs.refreshAndFindFileByPath(projectBasePath)
            ?: error("Project root not found: $projectBasePath")

        val rootVf = baseDir.findChild(rootName) ?: baseDir.createChildDirectory(this, rootName)
        val featureVf = rootVf.findChild(featureName) ?: rootVf.createChildDirectory(this, featureName)

        val modules = listOf("domain", "data", "di", "presentation")
        val created = mutableListOf<String>()
        val pathsToInclude = mutableListOf<String>()

        modules.forEach { name ->
            val existed = featureVf.findChild(name) != null
            if (!existed) {
                val moduleDir = VfsUtil.createDirectoryIfMissing(featureVf, name)
                    ?: error("Failed to create module directory: $name")
                moduleScaffolder.scaffoldModule(moduleDir, name, rootName, featureName)
                created.add(name)
            }
            pathsToInclude.add(GradlePathUtil.gradlePathFor(projectBasePath, featureVf.path, name))
        }

        settingsUpdater.updateRootSettingsIncludes(project, projectBasePath, pathsToInclude)
        featureVf.refresh(true, true)

        return if (created.isEmpty()) {
            "No modules were created because all exist for feature '$featureName'. Modules ensured in settings.gradle."
        } else {
            "Created modules: ${created.joinToString()} under feature '$featureName' and updated settings.gradle."
        }
    }
}
