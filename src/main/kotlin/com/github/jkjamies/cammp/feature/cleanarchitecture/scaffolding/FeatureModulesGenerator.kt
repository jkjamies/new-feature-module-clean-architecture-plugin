package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.cammp.feature.cleanarchitecture.util.GradlePathUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Paths

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

    private fun loadTemplateResource(path: String): String? {
        return this::class.java.getResource(path)?.readText()
            ?: this::class.java.classLoader.getResource(path.trimStart('/'))?.readText()
    }

    private fun applyPackagePlaceholder(text: String, safeOrg: String): String {
        // Replace both ${PACKAGE} and bare PACKAGE tokens.
        return text.replace(Regex("\\$\\{PACKAGE\\}"), safeOrg).replace("PACKAGE", safeOrg)
    }

    private fun createBuildLogicModule(projectBasePath: String, orgSegment: String, enabledModules: List<String>) {
        // Create build-logic directory at project root
        val lfs = LocalFileSystem.getInstance()
        val baseDir = lfs.refreshAndFindFileByPath(projectBasePath) ?: return
        val buildLogicVf = VfsUtil.createDirectoryIfMissing(baseDir, "build-logic") ?: return

        // copy settings.gradle.kts and build.gradle.kts templates
        val settingsText = loadTemplateResource("/templates/cleanArchitecture/buildLogic/settings.gradle.kts")
        if (settingsText != null) {
            if (buildLogicVf.findChild("settings.gradle.kts") == null) {
                buildLogicVf.createChildData(this, "settings.gradle.kts").let { VfsUtil.saveText(it, settingsText) }
            }
        }
        val buildTextTemplate = loadTemplateResource("/templates/cleanArchitecture/buildLogic/build.gradle.kts")
        if (buildTextTemplate != null) {
            val safeOrg = orgSegment.trim().ifEmpty { "jkjamies" }
            // Build plugin registrations only for enabled modules
            val registrations = buildString {
                append("gradlePlugin {\n    plugins {\n")
                fun reg(idSuffix: String, className: String, regName: String) {
                    append("        register(\"$regName\") {\n")
                    append("            id = \"com.$safeOrg.convention.android.library.$idSuffix\"\n")
                    append("            implementationClass = \"com.$safeOrg.convention.$className\"\n")
                    append("        }\n")
                }
                // Map modules to registrations
                if (enabledModules.contains("data")) reg("data", "DataConventionPlugin", "androidLibraryDataConvention")
                if (enabledModules.contains("di")) reg("di", "DiConventionPlugin", "androidLibraryDiConvention")
                if (enabledModules.contains("domain")) reg("domain", "DomainConventionPlugin", "androidLibraryDomainConvention")
                if (enabledModules.contains("presentation")) reg("presentation", "PresentationConventionPlugin", "androidLibraryPresentationConvention")
                if (enabledModules.contains("dataSource")) reg("dataSource", "DataSourceConventionPlugin", "androidLibraryDataSourceConvention")
                if (enabledModules.contains("remoteDataSource")) reg("remoteDataSource", "RemoteDataSourceConventionPlugin", "androidLibraryRemoteDataSourceConvention")
                if (enabledModules.contains("localDataSource")) reg("localDataSource", "LocalDataSourceConventionPlugin", "androidLibraryLocalDataSourceConvention")
                append("    }\n}\n")
            }
            // Replace PACKAGE tokens and inject registrations by replacing gradlePlugin block if present
            var buildText = applyPackagePlaceholder(buildTextTemplate, safeOrg)
            val gpIndex = buildText.indexOf("gradlePlugin {")
            if (gpIndex >= 0) {
                // Replace from gradlePlugin start to end of file (template ends with the block)
                buildText = buildText.substring(0, gpIndex) + registrations
            } else {
                // Append registrations if no block found
                buildText = buildText + "\n" + registrations
            }
            if (buildLogicVf.findChild("build.gradle.kts") == null) {
                buildLogicVf.createChildData(this, "build.gradle.kts").let { VfsUtil.saveText(it, buildText) }
            }
        }

        // create src/main/kotlin and copy convention plugin templates, replacing PACKAGE
        val srcMain = VfsUtil.createDirectoryIfMissing(buildLogicVf, "src/main/kotlin") ?: return
        val safeOrg = orgSegment.trim().ifEmpty { "jkjamies" }
        val packageDirPath = srcMain.path + "/com/" + safeOrg.replace('.', '/') + "/convention"
        val pkgDir = VfsUtil.createDirectories(packageDirPath)

        // Always include shared defaults
        val conventionFolderPath = "/templates/cleanArchitecture/buildLogic/conventionPlugins"
        listOf("AndroidLibraryDefaults.kt").forEach { fname ->
            val resPath = "$conventionFolderPath/$fname"
            val text = loadTemplateResource(resPath) ?: return@forEach
            val replaced = applyPackagePlaceholder(text, safeOrg)
            if (pkgDir.findChild(fname) == null) pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
        }
        // Conditionally include convention plugins matching enabled modules
        if (enabledModules.contains("data")) {
            listOf("DataConventionPlugin.kt").forEach { fname ->
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: return@forEach
                val replaced = applyPackagePlaceholder(text, safeOrg)
                if (pkgDir.findChild(fname) == null) {
                    pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
        }
        if (enabledModules.contains("di")) {
            listOf("DIConventionPlugin.kt").forEach { fname ->
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: return@forEach
                val replaced = applyPackagePlaceholder(text, safeOrg)
                if (pkgDir.findChild(fname) == null) {
                    pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
        }
        if (enabledModules.contains("domain")) {
            listOf("DomainConventionPlugin.kt").forEach { fname ->
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: return@forEach
                val replaced = applyPackagePlaceholder(text, safeOrg)
                if (pkgDir.findChild(fname) == null) {
                    pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
        }
        if (enabledModules.contains("presentation")) {
            listOf("PresentationConventionPlugin.kt").forEach { fname ->
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: return@forEach
                val replaced = applyPackagePlaceholder(text, safeOrg)
                if (pkgDir.findChild(fname) == null) {
                    pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
        }
        if (enabledModules.contains("dataSource") || enabledModules.contains("remoteDataSource") || enabledModules.contains("localDataSource")) {
            // include generic and specific datasource plugins as applicable
            if (enabledModules.contains("dataSource")) {
                val fname = "DataSourceConventionPlugin.kt"
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: ""
                if (text.isNotEmpty()) {
                    val replaced = applyPackagePlaceholder(text, safeOrg)
                    if (pkgDir.findChild(fname) == null) pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
            if (enabledModules.contains("remoteDataSource")) {
                val fname = "RemoteDataSourceConventionPlugin.kt"
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: ""
                if (text.isNotEmpty()) {
                    val replaced = applyPackagePlaceholder(text, safeOrg)
                    if (pkgDir.findChild(fname) == null) pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
            if (enabledModules.contains("localDataSource")) {
                val fname = "LocalDataSourceConventionPlugin.kt"
                val resPath = "$conventionFolderPath/$fname"
                val text = loadTemplateResource(resPath) ?: ""
                if (text.isNotEmpty()) {
                    val replaced = applyPackagePlaceholder(text, safeOrg)
                    if (pkgDir.findChild(fname) == null) pkgDir.createChildData(this, fname).let { VfsUtil.saveText(it, replaced) }
                }
            }
        }

        // refresh
        buildLogicVf.refresh(true, true)
    }

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
        includeDi: Boolean = true,
        orgSegment: String = "jkjamies"
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
                val md = VfsUtil.createDirectoryIfMissing(featureVf, name)
                    ?: error("Failed to create module directory: $name")
                // generate build file, src tree, and placeholder
                moduleScaffolder.scaffoldModule(
                    md,
                    name,
                    rootName,
                    featureName,
                    orgSegment,
                    includeDatasource = includeDatasource,
                    datasourceCombined = datasourceCombined,
                    datasourceRemote = datasourceRemote,
                    datasourceLocal = datasourceLocal
                )
                created.add(name)
            }
            // compute :root:feature:module path
            pathsToInclude.add(GradlePathUtil.gradlePathFor(projectBasePath, featureVf.path, name))
        }

        // idempotently add includes
        settingsUpdater.updateRootSettingsIncludes(projectBasePath, pathsToInclude)

        // Create build-logic module populated with convention plugins for the enabled layers
        val enabledModules = modules // the modules list indicates which conventions are relevant
        createBuildLogicModule(projectBasePath, orgSegment, enabledModules)

        // ensure settings includeBuild("build-logic") is present
        settingsUpdater.updateRootSettingsIncludeBuild(projectBasePath, "build-logic")

        // ensure the IDE reflects new directories/files
        featureVf.refresh(true, true)

        return if (created.isEmpty()) {
            "No modules were created because all exist for feature '$featureName'. Modules ensured in settings.gradle."
        } else {
            "Created modules: ${created.joinToString()} under feature '$featureName' and updated settings.gradle."
        }
    }
}
