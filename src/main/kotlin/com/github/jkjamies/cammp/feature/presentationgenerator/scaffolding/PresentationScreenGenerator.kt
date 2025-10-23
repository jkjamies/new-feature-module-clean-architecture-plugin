package com.github.jkjamies.cammp.feature.presentationgenerator.scaffolding

import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog.DiChoice
import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog.PatternChoice
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.Locale

/**
 * Scaffolds a presentation layer screen and optional navigation/state holder files.
 *
 * The generator operates under a selected directory within the project. If the selected directory
 * is a presentation module, it will create a screen folder (lower camel) and a ScreenName.kt file.
 * Based on options it can also create navigation host/destination and stateholders.
 */
class PresentationScreenGenerator(private val project: Project) {

    private fun loadTemplate(name: String): String {
        // Try Kotlin template first, then legacy .tpl fallback
        fun tryLoad(n: String): String? {
            val resourcePath = "/templates/presentationScreen/$n"
            return this::class.java.getResource(resourcePath)?.readText()
                ?: this::class.java.classLoader.getResource("templates/presentationScreen/$n")?.readText()
        }
        // Attempt exact name
        val exact = tryLoad(name)
        if (exact != null) return exact
        // Attempt counterpart extension (.kt <-> .tpl)
        val alt = when {
            name.endsWith(".kt") -> tryLoad(name.removeSuffix(".kt") + ".tpl")
            name.endsWith(".tpl") -> tryLoad(name.removeSuffix(".tpl") + ".kt")
            else -> null
        }
        return alt ?: error("Template not found: $name")
    }

    private fun renderTemplate(name: String, vars: Map<String, String>): String {
        var text = loadTemplate(name)
        // Replace placeholders of the form ${KEY} with provided values
        for ((k, v) in vars) {
            val placeholder = "\${$k}"
            text = text.replace(placeholder, v)
        }
        return text
    }

    private fun detectOrgSegment(presentationModuleDir: VirtualFile): String? {
        // 1) Check within this module: src/main/kotlin/com/<org>
        val inModule = VfsUtil.findRelativeFile("src/main/kotlin/com", presentationModuleDir)
        val firstChild = inModule?.children?.firstOrNull { it.isDirectory }
        if (firstChild != null) return firstChild.name

        // 2) Check sibling modules under the feature directory (the parent of this module)
        val featureDir = presentationModuleDir.parent
        val siblings = featureDir?.children?.filter { it.isDirectory && it.name != presentationModuleDir.name } ?: emptyList()
        for (sib in siblings) {
            val inSib = VfsUtil.findRelativeFile("src/main/kotlin/com", sib)
            val child = inSib?.children?.firstOrNull { it.isDirectory }
            if (child != null) return child.name
        }

        // 3) Give up (caller will fallback to default)
        return null
    }

    fun generate(
        projectBasePath: String,
        targetDirRelativeToProject: String,
        screenName: String,
        addNavigation: Boolean,
        useFlowStateHolder: Boolean,
        useScreenStateHolder: Boolean,
        diChoice: DiChoice,
        koinAnnotations: Boolean,
        patternChoice: PatternChoice,
        selectedUseCaseFqns: List<String> = emptyList(),
        selectedUseCaseModulePaths: Set<String> = emptySet()
    ): String {
        val lfs = LocalFileSystem.getInstance()
        val baseDir = lfs.refreshAndFindFileByPath(projectBasePath)
            ?: error("Project root not found: $projectBasePath")
        val targetDir = VfsUtil.createDirectoryIfMissing(baseDir, targetDirRelativeToProject)
            ?: error("Target directory not found/created: $targetDirRelativeToProject")

        // Determine module context names
        val moduleName = targetDir.name
        val parentModuleName = targetDir.parent?.name ?: moduleName
        val rootDirName = targetDir.parent?.parent?.name ?: "features"

        // compute base package directory under src/main/kotlin/com/<org>/<root>/<feature>/presentation
        val orgSegment = detectOrgSegment(targetDir) ?: "jkjamies"
        val pkgRelPath = "src/main/kotlin/com/$orgSegment/$rootDirName/$parentModuleName/presentation"
        val basePkgDir = VfsUtil.createDirectoryIfMissing(targetDir, pkgRelPath)
            ?: error("Failed to create package directory: $pkgRelPath")
        val basePkg = "com.$orgSegment.$rootDirName.$parentModuleName.presentation"

        // Compute folder name by omitting a trailing "Screen" from the screen name, then lowercasing the first letter
        val baseFolderSource = screenName.let {
            val suffix = "Screen"
            if (it.length >= suffix.length && it.endsWith(suffix, ignoreCase = true)) it.dropLast(suffix.length) else it
        }
        val folderName = baseFolderSource.replaceFirstChar { it.lowercase(Locale.getDefault()) }.ifBlank {
            // Fallback to original behavior if result is blank for some reason
            screenName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        }

        // If this looks like a presentation module, create screen subdir and file under base package dir
        val isPresentation = moduleName.equals("presentation", ignoreCase = true)
        val screenDir: VirtualFile? = if (isPresentation) {
            VfsUtil.createDirectoryIfMissing(basePkgDir, folderName)
        } else null

        if (isPresentation) {
            // Determine base name without trailing "Screen" to normalize naming
            val baseName = screenName.let {
                val suffix = "Screen"
                if (it.length >= suffix.length && it.endsWith(suffix, ignoreCase = true)) it.dropLast(suffix.length) else it
            }

            // Main screen file should always end with `Screen.kt`
            val fileName = "${baseName}Screen.kt"
            val screenKt = screenDir!!.findChild(fileName) ?: screenDir.createChildData(this, fileName)
            if (screenKt.length == 0L) {
                val content = renderTemplate("Screen.kt", mapOf(
                    "PACKAGE" to basePkg,
                    "FOLDER" to folderName,
                    "BASE_NAME" to baseName
                ))
                VfsUtil.saveText(screenKt, content)
            }

            // Additional screen-layer files: Intent (MVI only), ViewModel, UiState (omit trailing "Screen" in names)
            if (patternChoice == PatternChoice.MVI) {
                val intentName = "${baseName}Intent.kt"
                val intentFile = screenDir.findChild(intentName) ?: screenDir.createChildData(this, intentName)
                if (intentFile.length == 0L) VfsUtil.saveText(intentFile, renderTemplate("Intent.kt", mapOf(
                    "PACKAGE" to basePkg,
                    "FOLDER" to folderName,
                    "BASE_NAME" to baseName
                )))
            }

            // Compute imports and constructor params for ViewModel based on selected UseCases
            data class UseCaseRef(val simple: String, val fqn: String)
            val ucRefs = selectedUseCaseFqns.filter { it.isNotBlank() }.distinct().map { fqn ->
                val simple = fqn.substringAfterLast('.')
                UseCaseRef(simple = simple, fqn = fqn)
            }
            val imports = ucRefs
                .filter { it.fqn.contains('.') }
                .sortedBy { it.fqn }
                .joinToString("\n") { "import ${it.fqn}" }
            val constructorParams = ucRefs.joinToString(",\n    ") { ref ->
                val param = ref.simple.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                "private val $param: ${ref.simple}"
            }

            val viewModelName = "${baseName}ViewModel.kt"
            val viewModelFile = screenDir.findChild(viewModelName) ?: screenDir.createChildData(this, viewModelName)
            if (viewModelFile.length == 0L) VfsUtil.saveText(viewModelFile, renderTemplate("ViewModel.kt", mapOf(
                "PACKAGE" to basePkg,
                "FOLDER" to folderName,
                "BASE_NAME" to baseName,
                "IMPORTS" to imports,
                "CONSTRUCTOR_PARAMS" to constructorParams
            )))

            val uiStateName = "${baseName}UiState.kt"
            val uiStateFile = screenDir.findChild(uiStateName) ?: screenDir.createChildData(this, uiStateName)
            if (uiStateFile.length == 0L) VfsUtil.saveText(uiStateFile, renderTemplate("UiState.kt", mapOf(
                "PACKAGE" to basePkg,
                "FOLDER" to folderName,
                "BASE_NAME" to baseName
            )))
        }

        if (addNavigation) {
            // navigation directory under base package dir (or use parent module name before presentation)
            val navDir = VfsUtil.createDirectoryIfMissing(basePkgDir, "navigation")
            val rawPrefix = if (isPresentation) parentModuleName else moduleName
            val capPrefix = rawPrefix.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val navHostName = "${capPrefix}NavHost.kt"
            val navHost = navDir.findChild(navHostName) ?: navDir.createChildData(this, navHostName)
            if (navHost.length == 0L) VfsUtil.saveText(navHost, renderTemplate("NavHost.kt", mapOf(
                "PACKAGE" to basePkg,
                "NAV_HOST_NAME" to navHostName.removeSuffix(".kt")
            )))

            val destinationsDir = VfsUtil.createDirectoryIfMissing(navDir, "destinations")
            val destName = "${screenName}Destination.kt"
            val destFile = destinationsDir.findChild(destName) ?: destinationsDir.createChildData(this, destName)
            if (destFile.length == 0L) VfsUtil.saveText(destFile, renderTemplate("Destination.kt", mapOf(
                "PACKAGE" to basePkg,
                "SCREEN_NAME" to screenName,
                "ROUTE" to screenName.lowercase()
            )))
        }

        if (useFlowStateHolder) {
            val rawPrefix = if (isPresentation) parentModuleName else moduleName
            val capPrefix = rawPrefix.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val fshName = "${capPrefix}FlowStateHolder.kt"
            val hostDir = basePkgDir // place inside the base package directory
            val fshFile = hostDir.findChild(fshName) ?: hostDir.createChildData(this, fshName)
            if (fshFile.length == 0L) VfsUtil.saveText(fshFile, renderTemplate("FlowStateHolder.kt", mapOf(
                "PACKAGE" to basePkg,
                "FLOW_NAME" to fshName.removeSuffix(".kt")
            )))
        }

        if (useScreenStateHolder && isPresentation) {
            val sshName = "${screenName}StateHolder.kt"
            val sshFile = screenDir!!.findChild(sshName) ?: screenDir.createChildData(this, sshName)
            if (sshFile.length == 0L) VfsUtil.saveText(sshFile, renderTemplate("ScreenStateHolder.kt", mapOf(
                "PACKAGE" to basePkg,
                "FOLDER" to folderName,
                "SCREEN_NAME" to screenName
            )))
        }

        // Cross-feature usecase dependencies: add missing implementation(project(":"...)) entries
        if (selectedUseCaseModulePaths.isNotEmpty()) {
            addDependenciesForUseCases(targetDir, rootDirName, parentModuleName, selectedUseCaseModulePaths)
        }

        targetDir.refresh(true, true)
        return "Presentation screen '$screenName' generated under $targetDirRelativeToProject."
    }

    // Backwards-compatible overload to preserve the original signature used by tests
    fun generate(
        projectBasePath: String,
        targetDirRelativeToProject: String,
        screenName: String,
        addNavigation: Boolean,
        useFlowStateHolder: Boolean,
        useScreenStateHolder: Boolean,
        diChoice: DiChoice,
        koinAnnotations: Boolean,
        patternChoice: PatternChoice
    ): String = generate(
        projectBasePath = projectBasePath,
        targetDirRelativeToProject = targetDirRelativeToProject,
        screenName = screenName,
        addNavigation = addNavigation,
        useFlowStateHolder = useFlowStateHolder,
        useScreenStateHolder = useScreenStateHolder,
        diChoice = diChoice,
        koinAnnotations = koinAnnotations,
        patternChoice = patternChoice,
        selectedUseCaseFqns = emptyList(),
        selectedUseCaseModulePaths = emptySet()
    )

    private fun addDependenciesForUseCases(
        moduleDir: VirtualFile,
        rootDirName: String,
        currentFeatureName: String,
        selectedUseCaseModulePaths: Set<String>
    ) {
        // Consider any selected module that is not under the same feature submodules
        val sameFeaturePrefix = ":$rootDirName:$currentFeatureName"
        val crossModuleDeps = selectedUseCaseModulePaths.filter { gradlePath ->
            // exclude if it's the same feature module path or a submodule of it
            !(gradlePath == sameFeaturePrefix || gradlePath.startsWith("$sameFeaturePrefix:"))
        }.toSet()
        if (crossModuleDeps.isEmpty()) return

        val buildFile = moduleDir.findChild("build.gradle.kts")
            ?: moduleDir.findChild("build.gradle")
            ?: return
        val original = VfsUtil.loadText(buildFile)

        // Skip those already present
        val missing = crossModuleDeps.filterNot { dep -> original.contains("project(\"$dep\")") }
        if (missing.isEmpty()) return

        val updated = insertDependencies(original, missing)
        if (updated != original) {
            VfsUtil.saveText(buildFile, updated)
        }
    }

    private fun insertDependencies(original: String, modulePaths: Collection<String>): String {
        val depDecls = modulePaths.joinToString("\n") { "    implementation(project(\"$it\"))" }
        val regex = Regex("dependencies\\s*\\{")
        val match = regex.find(original)
        return if (match != null) {
            val start = match.range.last + 1 // position after '{'
            val end = findMatchingBrace(original, match.range.last)
            if (end > start) {
                val before = original.substring(0, end)
                val after = original.substring(end)
                // Insert before the closing brace with a preceding newline
                before + "\n" + depDecls + "\n" + after
            } else {
                // Fallback: append a new dependencies block
                original.trimEnd() + "\n\n" + "dependencies {\n$depDecls\n}\n"
            }
        } else {
            // No dependencies block; append one at end
            original.trimEnd() + "\n\n" + "dependencies {\n$depDecls\n}\n"
        }
    }

    private fun findMatchingBrace(text: String, openBraceIndex: Int): Int {
        // openBraceIndex should be at the position of '{'
        var depth = 0
        for (i in openBraceIndex until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
