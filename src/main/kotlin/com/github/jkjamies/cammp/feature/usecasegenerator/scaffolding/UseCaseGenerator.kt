package com.github.jkjamies.cammp.feature.usecasegenerator.scaffolding

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.Locale

class UseCaseGenerator(@Suppress("unused") private val project: Project) {

    private fun loadTemplate(name: String): String {
        fun tryLoad(n: String): String? {
            val resourcePath = "/templates/usecaseGenerator/$n"
            return this::class.java.getResource(resourcePath)?.readText()
                ?: this::class.java.classLoader.getResource("templates/usecaseGenerator/$n")?.readText()
        }
        val exact = tryLoad(name)
        if (exact != null) return exact
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
            text = text.replace($$"${$$k}", v)
        }
        return text
    }

    private fun detectOrgSegment(moduleDir: VirtualFile): String? {
        // Look for src/main/kotlin/com/<org>
        val inModule = VfsUtil.findRelativeFile("src/main/kotlin/com", moduleDir)
        val firstChild = inModule?.children?.firstOrNull { it.isDirectory }
        if (firstChild != null) return firstChild.name

        // Fallback: check siblings
        val featureDir = moduleDir.parent
        val siblings = featureDir?.children?.filter { it.isDirectory && it.name != moduleDir.name } ?: emptyList()
        for (sib in siblings) {
            val inSib = VfsUtil.findRelativeFile("src/main/kotlin/com", sib)
            val child = inSib?.children?.firstOrNull { it.isDirectory }
            if (child != null) return child.name
        }
        return null
    }

    fun generate(
        projectBasePath: String,
        domainModuleDirRelativeToProject: String,
        useCaseName: String,
        diEnabled: Boolean = true,
        useHilt: Boolean = true,
        koinAnnotations: Boolean = false,
        selectedRepositories: List<String> = emptyList()
    ): String {
        val lfs = LocalFileSystem.getInstance()
        val baseDir = lfs.refreshAndFindFileByPath(projectBasePath)
            ?: error("Project root not found: $projectBasePath")
        val domainModuleDir = VfsUtil.createDirectoryIfMissing(baseDir, domainModuleDirRelativeToProject)
            ?: error("Target directory not found/created: $domainModuleDirRelativeToProject")

        val moduleName = domainModuleDir.name
        if (!moduleName.equals("domain", ignoreCase = true)) {
            error("Selected directory must be the domain module")
        }

        val featureDir = domainModuleDir.parent ?: error("Could not resolve feature directory from domain module")

        // Determine names for base package
        val rootDirName = featureDir.parent?.name ?: "features"
        val featureName = featureDir.name
        val orgSegment = detectOrgSegment(domainModuleDir) ?: "jkjamies"

        val domainPkgRel = "src/main/kotlin/com/$orgSegment/$rootDirName/$featureName/domain/usecase"
        val domainPkgDir = VfsUtil.createDirectoryIfMissing(domainModuleDir, domainPkgRel)
            ?: error("Failed to create package directory: $domainPkgRel")

        val domainBasePkg = "com.$orgSegment.$rootDirName.$featureName.domain.usecase"
        val repoBasePkg = "com.$orgSegment.$rootDirName.$featureName.domain.repository"

        val normalizedName = useCaseName.trim().let { raw ->
            val capped = raw.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            if (capped.endsWith("UseCase", ignoreCase = true)) capped else capped + "UseCase"
        }
        val fileName = "$normalizedName.kt"
        val file = domainPkgDir.findChild(fileName) ?: domainPkgDir.createChildData(this, fileName)
        if (file.length == 0L) {
            val content = if (diEnabled && useHilt) {
                data class RepoRef(val simple: String, val fqn: String)
                val repoRefs = selectedRepositories.mapNotNull { sel ->
                    if (sel.isBlank()) null else {
                        if (sel.contains('.')) {
                            val simple = sel.substringAfterLast('.')
                            RepoRef(simple, sel)
                        } else {
                            RepoRef(sel, "$repoBasePkg.$sel")
                        }
                    }
                }
                val imports = if (repoRefs.isNotEmpty()) repoRefs.joinToString("\n") { "import ${it.fqn}" } else ""
                val constructorParams = if (repoRefs.isNotEmpty())
                    repoRefs.joinToString(",\n    ") { r ->
                        val param = r.simple.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                        "private val $param: ${r.simple}"
                    }
                else ""
                renderTemplate(
                    "UseCase.kt",
                    mapOf(
                        "PACKAGE" to domainBasePkg,
                        "USECASE_NAME" to normalizedName,
                        "IMPORTS" to imports,
                        "CONSTRUCTOR_PARAMS" to constructorParams
                    )
                )
            } else {
                "// TODO: not yet available"
            }
            VfsUtil.saveText(file, content)
        }

        domainModuleDir.refresh(true, true)
        return "UseCase '$normalizedName' generated in domain module."
    }
}
