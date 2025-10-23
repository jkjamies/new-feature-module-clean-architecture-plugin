package com.github.jkjamies.cammp.feature.repositorygenerator.scaffolding

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.Locale

/**
 * Generates a domain Repository interface and a data-layer implementation, and optionally
 * a DI binding module. Uses IntelliJ VFS (LocalFileSystem/VfsUtil) and must be called
 * from a write action.
 */
class RepositoryGenerator(@Suppress("unused") private val project: Project) {

    private fun loadTemplate(name: String): String {
        fun tryLoad(n: String): String? {
            val resourcePath = "/templates/repositoryGenerator/$n"
            return this::class.java.getResource(resourcePath)?.readText()
                ?: this::class.java.classLoader.getResource("templates/repositoryGenerator/$n")?.readText()
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
            val placeholder = "\${$k}"
            text = text.replace(placeholder, v)
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
        dataModuleDirRelativeToProject: String,
        repositoryName: String,
        diEnabled: Boolean = false,
        useHilt: Boolean = true,
        koinAnnotations: Boolean = false
    ): String {
        val lfs = LocalFileSystem.getInstance()
        val baseDir = lfs.refreshAndFindFileByPath(projectBasePath)
            ?: error("Project root not found: $projectBasePath")
        val dataModuleDir = VfsUtil.createDirectoryIfMissing(baseDir, dataModuleDirRelativeToProject)
            ?: error("Target directory not found/created: $dataModuleDirRelativeToProject")

        val moduleName = dataModuleDir.name
        if (!moduleName.equals("data", ignoreCase = true)) {
            error("Selected directory must be the data module")
        }

        val featureDir = dataModuleDir.parent ?: error("Could not resolve feature directory from data module")
        val domainModuleDir = featureDir.findChild("domain")
            ?: VfsUtil.createDirectoryIfMissing(featureDir, "domain")
            ?: error("Failed to locate or create domain module directory")

        // Determine names for base package
        val rootDirName = featureDir.parent?.name ?: "features"
        val featureName = featureDir.name
        val orgSegment = detectOrgSegment(dataModuleDir) ?: "jkjamies"

        // Compute module base package directories
        val dataPkgRel = "src/main/kotlin/com/$orgSegment/$rootDirName/$featureName/data"
        val domainPkgRel = "src/main/kotlin/com/$orgSegment/$rootDirName/$featureName/domain"
        val dataPkgDir = VfsUtil.createDirectoryIfMissing(dataModuleDir, dataPkgRel)
            ?: error("Failed to create package directory: $dataPkgRel")
        val domainPkgDir = VfsUtil.createDirectoryIfMissing(domainModuleDir, domainPkgRel)
            ?: error("Failed to create package directory: $domainPkgRel")

        val dataBasePkg = "com.$orgSegment.$rootDirName.$featureName.data"
        val domainBasePkg = "com.$orgSegment.$rootDirName.$featureName.domain"

        // Ensure repository directories
        val dataRepoDir = VfsUtil.createDirectoryIfMissing(dataPkgDir, "repository")
            ?: error("Failed to create data repository directory")
        val domainRepoDir = VfsUtil.createDirectoryIfMissing(domainPkgDir, "repository")
            ?: error("Failed to create domain repository directory")

        // Normalize repository class names
        val repoSimpleName = repositoryName.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            .removeSuffix("Impl") // avoid accidental Impl suffix in interface name
        val repoImplName = repoSimpleName + "Impl"

        // Domain interface
        val domainFileName = "$repoSimpleName.kt"
        val domainFile = domainRepoDir.findChild(domainFileName) ?: domainRepoDir.createChildData(this, domainFileName)
        if (domainFile.length == 0L) {
            val domainContent = renderTemplate("Repository.kt", mapOf(
                "PACKAGE" to domainBasePkg,
                "REPOSITORY_NAME" to repoSimpleName
            ))
            VfsUtil.saveText(domainFile, domainContent)
        }

        // Data implementation
        val dataFileName = "$repoImplName.kt"
        val dataFile = dataRepoDir.findChild(dataFileName) ?: dataRepoDir.createChildData(this, dataFileName)
        if (dataFile.length == 0L) {
            val dataContent = renderTemplate("RepositoryImpl.kt", mapOf(
                "PACKAGE" to dataBasePkg,
                "DOMAIN_PACKAGE" to domainBasePkg,
                "REPOSITORY_NAME" to repoSimpleName,
                "REPOSITORY_IMPL_NAME" to repoImplName
            ))
            VfsUtil.saveText(dataFile, dataContent)
        }

        // DI: Hilt binding module in di module when requested
        if (diEnabled && useHilt) {
            val diModuleDir = featureDir.findChild("di")
                ?: VfsUtil.createDirectoryIfMissing(featureDir, "di")
                ?: error("Failed to locate or create di module directory")

            val diPkgRel = "src/main/kotlin/com/$orgSegment/$rootDirName/$featureName/di"
            val diPkgDir = VfsUtil.createDirectoryIfMissing(diModuleDir, diPkgRel)
                ?: error("Failed to create package directory: $diPkgRel")
            val diRepoDir = VfsUtil.createDirectoryIfMissing(diPkgDir, "repository")
                ?: error("Failed to create di repository directory")

            val moduleFile = diRepoDir.findChild("RepositoryModule.kt")
                ?: diRepoDir.createChildData(this, "RepositoryModule.kt")
            if (moduleFile.length == 0L) {
                val diBasePkg = "com.$orgSegment.$rootDirName.$featureName.di"
                val content = renderTemplate("RepositoryModule.kt", mapOf(
                    "PACKAGE" to diBasePkg,
                    "DOMAIN_PACKAGE" to domainBasePkg,
                    "DATA_PACKAGE" to dataBasePkg,
                    "REPOSITORY_NAME" to repoSimpleName,
                    "REPOSITORY_IMPL_NAME" to repoImplName
                ))
                VfsUtil.saveText(moduleFile, content)
            }
            diModuleDir.refresh(true, true)
        }

        dataModuleDir.refresh(true, true)
        domainModuleDir.refresh(true, true)
        return "Repository '$repoSimpleName' generated in domain and data modules." + if (diEnabled && useHilt) " Hilt module created." else ""
    }
}
