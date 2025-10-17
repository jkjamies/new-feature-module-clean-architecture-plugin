package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util.GradlePathUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

/**
 * Coordinates scaffolding of feature modules and updating Gradle settings.
 *
 * This class delegates to [ModuleScaffolder] for file layout and to [SettingsUpdater]
 * for updating settings.gradle(.kts). It relies on the IntelliJ VFS ([LocalFileSystem], [VfsUtil])
 * and should be called from a writing context.
 */
class FeatureModulesGenerator(private val project: Project) {
    private val moduleScaffolder = ModuleScaffolder()
    private val settingsUpdater = SettingsUpdater()

    /**
     * Generates the standard module set under the given root/feature, creating missing
     * directories and ensuring includes in the root settings file.
     *
     * @param projectBasePath absolute path to the IDE project root as returned by [Project.basePath]
     * @param rootName folder under the project root that contains features (for example, "features")
     * @param featureName the feature folder name to create or reuse
     * @param includePresentation whether to include "presentation" module
     * @param includeDatasource whether to include datasource-related modules
     * @param datasourceCombined if true, include a single "dataSource" module
     * @param datasourceRemote if true, include a "remoteDataSource" module (ignored when combined)
     * @param datasourceLocal if true, include a "localDataSource" module (ignored when combined)
     * @param includeDi whether to include the "di" module
     * @return human-friendly summary of the work performed
     */
    fun generate(
        projectBasePath: String,
        rootName: String,
        featureName: String,
        includePresentation: Boolean = true,
        includeDatasource: Boolean = false,
        datasourceCombined: Boolean = false,
        datasourceRemote: Boolean = false,
        datasourceLocal: Boolean = false,
        includeDi: Boolean = true
    ): String = generate(
        projectBasePath,
        rootName,
        featureName,
        includePresentation,
        includeDatasource,
        datasourceCombined,
        datasourceRemote,
        datasourceLocal,
        includeDi,
        orgSegment = "jkjamies"
    )

    fun generate(
        projectBasePath: String,
        rootName: String,
        featureName: String,
        includePresentation: Boolean = true,
        includeDatasource: Boolean = false,
        datasourceCombined: Boolean = false,
        datasourceRemote: Boolean = false,
        datasourceLocal: Boolean = false,
        includeDi: Boolean = true,
        orgSegment: String
    ): String {
        val lfs = LocalFileSystem.getInstance()
        val baseDir = lfs.refreshAndFindFileByPath(projectBasePath)
            // ensure VFS sees the project root
            ?: error("Project root not found: $projectBasePath")

        // create features root if absent (supports nested paths like "features/shared")
        val rootVf = VfsUtil.createDirectoryIfMissing(baseDir, rootName)
            ?: error("Failed to access or create root directory: $rootName")
        // create specific feature dir if absent
        val featureVf = VfsUtil.createDirectoryIfMissing(rootVf, featureName)
            ?: error("Failed to access or create feature directory: $featureName")

        val baseModules = mutableListOf("domain", "data")
        if (includeDi) baseModules.add("di")
        if (includePresentation) baseModules.add("presentation")
        // Datasource modules based on flags
        if (includeDatasource) {
            if (datasourceCombined) {
                baseModules.add("dataSource")
            } else {
                if (datasourceRemote) baseModules.add("remoteDataSource")
                if (datasourceLocal) baseModules.add("localDataSource")
            }
        }
        val modules = baseModules.toList()
        // track which modules we actually created
        val created = mutableListOf<String>()
        // collect Gradle include paths for settings.gradle
        val pathsToInclude = mutableListOf<String>()

        modules.forEach { name ->
            // check presence via VFS to avoid re-scaffolding
            val existed = featureVf.findChild(name) != null
            if (!existed) {
                val moduleDir = VfsUtil.createDirectoryIfMissing(featureVf, name)
                    // defensive: VFS could return null on failure
                    ?: error("Failed to create module directory: $name")
                // generate build file, src tree, and placeholder
                moduleScaffolder.scaffoldModule(moduleDir, name, rootName, featureName, orgSegment)
                created.add(name)
            }
            // compute :root:feature:module path
            pathsToInclude.add(GradlePathUtil.gradlePathFor(projectBasePath, featureVf.path, name))
        }

        // idempotently add includes
        settingsUpdater.updateRootSettingsIncludes(projectBasePath, pathsToInclude)
        // ensure the IDE reflects new directories/files
        featureVf.refresh(true, true)

        return if (created.isEmpty()) {
            "No modules were created because all exist for feature '$featureName'. Modules ensured in settings.gradle."
        } else {
            "Created modules: ${created.joinToString()} under feature '$featureName' and updated settings.gradle."
        }
    }
}
