package com.github.jkjamies.newfeaturemodule.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel

class GenerateModulesAction : AnAction(
    "Generate Clean Architecture Modules",
    "Create domain, data, di, and presentation modules inside the current project",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = GenerateModulesDialog(project)
        if (!dialog.showAndGet()) return

        val rootNameOpt = dialog.getRootFolderName()
        val featureNameOpt = dialog.getFeatureName()
        if (rootNameOpt == null || featureNameOpt == null) {
            Messages.showErrorDialog(project, "Please enter both the root folder and the subdirectory (feature) name.", "Invalid Input")
            return
        }
        val rootName = rootNameOpt
        val featureName = featureNameOpt

        val projectBasePath = project.basePath
        if (projectBasePath == null) {
            Messages.showErrorDialog(project, "Cannot resolve project base path.", "Error")
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val lfs = LocalFileSystem.getInstance()
                val baseDir = lfs.refreshAndFindFileByPath(projectBasePath)
                    ?: throw IllegalStateException("Project root not found: $projectBasePath")

                // Ensure root folder under project root, then feature subfolder under it
                val rootVf = baseDir.findChild(rootName) ?: baseDir.createChildDirectory(this, rootName)
                val featureVf = rootVf.findChild(featureName) ?: rootVf.createChildDirectory(this, featureName)

                val modules = listOf("domain", "data", "di", "presentation")
                val createdModules = mutableListOf<String>()
                val modulesToInclude = mutableListOf<String>()

                modules.forEach { moduleName ->
                    val existed = featureVf.findChild(moduleName) != null
                    if (!existed) {
                        val moduleDir = VfsUtil.createDirectoryIfMissing(featureVf, moduleName)
                            ?: throw IllegalStateException("Failed to create module directory: $moduleName")
                        scaffoldModule(moduleDir, moduleName, rootName, featureName)
                        createdModules.add(moduleName)
                    }
                    // Always ensure include exists for this module (even if it already existed on disk)
                    modulesToInclude.add(gradlePathFor(projectBasePath, featureVf.path, moduleName))
                }

                // Update root settings.gradle(.kts) with includes for these modules
                updateRootSettingsIncludes(project, projectBasePath, modulesToInclude)

                featureVf.refresh(true, true)
                val msg = if (createdModules.isEmpty()) {
                    "No modules were created because all exist for feature '$featureName'. Modules ensured in settings.gradle."
                } else {
                    "Created modules: ${createdModules.joinToString()} under feature '$featureName' and updated settings.gradle."
                }
                Messages.showInfoMessage(project, msg, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate modules: ${t.message}", "Error")
            }
        }
    }

    private fun gradlePathFor(projectBasePath: String, featurePath: String, moduleName: String): String {
        val rel = featurePath.removePrefix(projectBasePath).trimStart('/', '\\')
        val segments = (if (rel.isEmpty()) listOf() else rel.split('/', '\\')) + moduleName
        return ":" + segments.joinToString(":")
    }

    private fun updateRootSettingsIncludes(project: Project, projectBasePath: String, moduleGradlePaths: List<String>) {
        val lfs = LocalFileSystem.getInstance()
        val projectRootVf = lfs.findFileByPath(projectBasePath)
            ?: throw IllegalStateException("Project root not found: $projectBasePath")

        // Prefer Kotlin DSL if present
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

    private fun scaffoldModule(moduleDir: VirtualFile, moduleName: String, rootName: String, featureName: String) {
        // build.gradle.kts for module
        val buildText = """
            plugins {
                `java-library`
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }
            
            group = "com.example"
            version = "1.0.0"
            
            repositories {
                mavenCentral()
                google()
            }
            
            // Add your module-specific dependencies here
            dependencies {
            }
        """.trimIndent()
        writeFileIfAbsent(moduleDir, "build.gradle.kts", buildText)

        // basic src structure (use Kotlin sources directories)
        val srcMainKotlin = VfsUtil.createDirectories(moduleDir.path + "/src/main/kotlin")
        val srcMainResources = VfsUtil.createDirectories(moduleDir.path + "/src/main/resources")
        val srcTestKotlin = VfsUtil.createDirectories(moduleDir.path + "/src/test/kotlin")
        srcMainKotlin.refresh(false, true)
        srcMainResources.refresh(false, true)
        srcTestKotlin.refresh(false, true)

        // placeholder Kotlin class to ensure folders are visible
        val packageName = "com.jkjamies.$rootName.$featureName.$moduleName"
        val placeholder = """
            package ${'$'}packageName
            
            class Placeholder
        """.trimIndent()
        val pkgDirPath = srcMainKotlin.path + "/" + packageName.replace('.', '/')
        val pkgDir = VfsUtil.createDirectories(pkgDirPath)
        writeFileIfAbsent(pkgDir, "Placeholder.kt", placeholder)
    }

    private fun writeFileIfAbsent(parent: VirtualFile, name: String, content: String) {
        val existing = parent.findChild(name)
        if (existing == null) {
            val file = parent.createChildData(this, name)
            VfsUtil.saveText(file, content)
        }
    }
}


// Dialog to collect root folder and subdirectory (feature) name
private class GenerateModulesDialog(private val project: Project) : DialogWrapper(project, true) {
    private val rootPanel = JPanel(GridBagLayout())
    private val rootField = JBTextField()
    private val featureField = JBTextField()

    init {
        title = "Generate Feature Modules"
        initUi()
        init()
    }

    private fun initUi() {
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = Insets(8, 8, 8, 8)
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        // Row 0: Root folder label
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        rootPanel.add(JBLabel("Root folder under project root:"), gbc)

        // Row 1: Root folder field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0
        rootPanel.add(rootField, gbc)

        // Row 2: Feature subdirectory label
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        rootPanel.add(JBLabel("Feature subdirectory name:"), gbc)

        // Row 3: Feature field
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1.0
        rootPanel.add(featureField, gbc)
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(rootPanel, BorderLayout.CENTER)
        }
    }

    fun getRootFolderName(): String? {
        return rootField.text?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun getFeatureName(): String? {
        return featureField.text?.trim()?.takeIf { it.isNotEmpty() }
    }
}
