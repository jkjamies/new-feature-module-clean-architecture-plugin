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
        for ((k, v) in vars) {
            text = text.replace("\${'$'}{$k}", v)
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
        patternChoice: PatternChoice
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

            // Determine base name without trailing "Screen" for ancillary files

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

            val viewModelName = "${baseName}ViewModel.kt"
            val viewModelFile = screenDir.findChild(viewModelName) ?: screenDir.createChildData(this, viewModelName)
            if (viewModelFile.length == 0L) VfsUtil.saveText(viewModelFile, renderTemplate("ViewModel.kt", mapOf(
                "PACKAGE" to basePkg,
                "FOLDER" to folderName,
                "BASE_NAME" to baseName
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

        // DI options are currently UI-only; keep feature separate from clean architecture generator
        targetDir.refresh(true, true)
        return "Presentation screen '$screenName' generated under $targetDirRelativeToProject."
    }

    private fun defaultScreenContent(name: String, basePkg: String, folder: String) = """
        package $basePkg.$folder
        
        import androidx.compose.runtime.Composable
        
        @Composable
        fun ${name}Screen() {
            // TODO: implement $name screen UI
        }
    """.trimIndent()

    private fun defaultNavHostContent(name: String, basePkg: String) = """
        package $basePkg.navigation
        
        import androidx.compose.runtime.Composable
        import androidx.navigation.compose.NavHost
        import androidx.navigation.compose.rememberNavController
        
        @Composable
        fun $name() {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "TODO") {
                // TODO: add destinations
            }
        }
    """.trimIndent()

    private fun defaultDestinationContent(screenName: String, basePkg: String) = """
        package $basePkg.navigation.destinations
        
        object ${screenName}Destination {
            const val route = "${screenName.lowercase()}"
        }
    """.trimIndent()

    private fun defaultFlowStateHolderContent(name: String, basePkg: String) = """
        package $basePkg
        
        import kotlinx.coroutines.flow.MutableStateFlow
        import kotlinx.coroutines.flow.StateFlow
        
        class $name {
            private val _state = MutableStateFlow(Unit)
            val state: StateFlow<Unit> = _state
        }
    """.trimIndent()

    private fun defaultScreenStateHolderContent(screenName: String, basePkg: String, folder: String) = """
        package $basePkg.$folder
        
        class ${screenName}StateHolder {
            // TODO: state for $screenName
        }
    """.trimIndent()

    private fun defaultIntentContent(screenName: String, basePkg: String, folder: String) = """
        package $basePkg.$folder
        
        sealed interface ${screenName}Intent {
            // Define user intents for $screenName screen
        }
    """.trimIndent()

    private fun defaultViewModelContent(screenName: String, basePkg: String, folder: String) = """
        package $basePkg.$folder
        
        import androidx.lifecycle.ViewModel
        
        class ${screenName}ViewModel : ViewModel() {
            // TODO: implement ViewModel logic for $screenName
        }
    """.trimIndent()

    private fun defaultUiStateContent(screenName: String, basePkg: String, folder: String) = """
        package $basePkg.$folder
        
        data class ${screenName}UiState(
            val isLoading: Boolean = false
        )
    """.trimIndent()
}
