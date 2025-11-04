package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.cammp.feature.cleanarchitecture.util.GradlePathUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
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

    private fun normalizeToAbsolute(projectBasePath: String, inputPath: String): String {
        val p = try { Paths.get(inputPath) } catch (t: Throwable) { null }
        val abs = if (p?.isAbsolute == true) p.normalize() else Paths.get(projectBasePath, inputPath).normalize()
        return abs.toString().replace('\\', '/')
    }

    private fun loadRootTemplateTextFor(moduleName: String): String {
        val name = when (moduleName) {
            "domain" -> "domain"
            "data" -> "data"
            "di" -> "di"
            "presentation" -> "presentation"
            "dataSource" -> "dataSource"
            "remoteDataSource" -> "remoteDataSource"
            "localDataSource" -> "localDataSource"
            else -> "domain"
        }
        // Root templates are always under templates/cleanArchitecture/root
        val candidates = listOf(
            "/templates/cleanArchitecture/root/$name.gradle.kts",
            "templates/cleanArchitecture/root/$name.gradle.kts"
        )
        candidates.forEach { p ->
            try {
                val viaRes = this::class.java.getResource(p)?.readText()
                if (viaRes != null) return viaRes
                val viaCl = this::class.java.classLoader.getResource(p.removePrefix("/"))?.readText()
                if (viaCl != null) return viaCl
            } catch (_: Throwable) { }
        }
        return "" // fallback to blank if unavailable
    }

    /**
     * Pre-create root Gradle script files once across all selected modules according to rules:
     * - If a path is selected by multiple modules, create a single blank file (if missing).
     * - If a path is selected by only one module, create from the corresponding root template (if missing).
     * Never overwrite existing files.
     */
    private fun prepareRootScripts(
        projectBasePath: String,
        selections: Map<String, String>
    ) {
        if (selections.isEmpty()) return
        // Normalize all paths to absolute Unix-style and group by target file
        data class Target(val absPath: String, val fileName: String, val dirPath: String)
        val grouped = mutableMapOf<String, MutableList<String>>() // absPath -> moduleNames
        val targets = mutableMapOf<String, Target>()
        selections.forEach { (moduleName, input) ->
            val abs = normalizeToAbsolute(projectBasePath, input)
            val dir = abs.substringBeforeLast('/', projectBasePath.replace('\\','/'))
            val file = abs.substringAfterLast('/')
            grouped.getOrPut(abs) { mutableListOf() }.add(moduleName)
            targets.putIfAbsent(abs, Target(abs, file, dir))
        }
        grouped.forEach { (abs, modules) ->
            val t = targets[abs] ?: return@forEach
            val parentDirVf = VfsUtil.createDirectories(t.dirPath)
            val existing = parentDirVf.findChild(t.fileName)
            if (existing != null) return@forEach // do not overwrite
            val file = parentDirVf.createChildData(this, t.fileName)
            val content = if (modules.size > 1) {
                "" // blank file for duplicates
            } else {
                loadRootTemplateTextFor(modules.first())
            }
            VfsUtil.saveText(file, content)
        }
    }

    private fun ensureApplyAtTop(
        projectBasePath: String,
        moduleDir: VirtualFile,
        scriptPathInput: String
    ) {
        val fullPathUnix = normalizeToAbsolute(projectBasePath, scriptPathInput)
        // Compute project-relative path robustly
        val relPathUnix = try {
            val basePath = Paths.get(projectBasePath).toAbsolutePath().normalize()
            val targetPath = Paths.get(fullPathUnix).toAbsolutePath().normalize()
            if (targetPath.startsWith(basePath)) basePath.relativize(targetPath).toString().replace('\\', '/') else fullPathUnix
        } catch (t: Throwable) {
            // Fallback to previous logic if anything goes wrong
            val baseUnix = projectBasePath.replace('\\', '/')
            if (fullPathUnix.startsWith(baseUnix)) fullPathUnix.removePrefix(baseUnix).trimStart('/') else fullPathUnix
        }
        // Ensure apply(from = rootProject.file("...")) is present in module build.gradle.kts
        val buildFile = moduleDir.findChild("build.gradle.kts") ?: moduleDir.createChildData(this, "build.gradle.kts")
        val current = try { VfsUtil.loadText(buildFile) } catch (_: Throwable) { "" }
        val applyLine = "apply(from = rootProject.file(\"$relPathUnix\"))"
        val already = current.contains(applyLine)
        if (!already) {
            val updated = if (current.isEmpty()) {
                applyLine + "\n"
            } else {
                // Ensure a blank line between the apply and the rest of the file
                applyLine + "\n\n" + current
            }
            VfsUtil.saveText(buildFile, updated)
        }
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
     * @param rootScripts optional mapping of module name to a root Gradle script file path (absolute or project-relative)
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
        orgSegment: String = "jkjamies",
        rootScripts: Map<String, String> = emptyMap()
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

        // Pre-create root script files once based on selections (templates vs blank for duplicates)
        prepareRootScripts(projectBasePath, rootScripts)

        modules.forEach { name ->
            // check presence via VFS to avoid re-scaffolding
            val existed = featureVf.findChild(name) != null
            val moduleDir = if (!existed) {
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
                md
            } else {
                featureVf.findChild(name) ?: error("Module directory not found after existence check: $name")
            }

            // If root script mapping provided for this module, ensure script exists and apply-from is added
            val scriptPathInput = rootScripts[name]
            if (!scriptPathInput.isNullOrBlank()) {
                ensureApplyAtTop(projectBasePath, moduleDir, scriptPathInput)
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
