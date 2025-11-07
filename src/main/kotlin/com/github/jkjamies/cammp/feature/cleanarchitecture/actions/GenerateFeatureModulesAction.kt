package com.github.jkjamies.cammp.feature.cleanarchitecture.actions

import com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding.FeatureModulesGenerator
import com.github.jkjamies.cammp.feature.cleanarchitecture.ui.GenerateModulesDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Paths

/**
 * Action entry point that opens [GenerateModulesDialog] and scaffolds feature modules.
 *
 * The action is registered in plugin.xml and appears in the Tools menu. It collects
 * user input, validates project base path from [Project.basePath], then performs all
 * file-system updates inside a write command via [WriteCommandAction].
 */
class GenerateFeatureModulesAction : AnAction("Generate Clean Architecture Modules") {
    /**
     * Handles the user invocation from the IDE UI with the given [AnActionEvent].
     *
     * - Acquires the [Project] from [AnActionEvent.project].
     * - Shows [GenerateModulesDialog].
     * - Delegates generation to [FeatureModulesGenerator.generate].
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dialog = GenerateModulesDialog(project)
        if (!dialog.showAndGet()) return

        val (rootInput, featureName) = dialog.getValues()
        val orgSegment = dialog.getOrgSegment()
        val includePresentation = dialog.getIncludePresentation()
        val includeDatasource = dialog.getIncludeDatasource()
        val dsCombined = dialog.isDatasourceCombinedSelected()
        val dsRemote = dialog.isDatasourceRemoteSelected()
        val dsLocal = dialog.isDatasourceLocalSelected()
        val includeDi = dialog.getIncludeDi()
        val basePath = project.basePath ?: run {
            Messages.showErrorDialog(project, "Project base path not found", "Error")
            return
        }

        // Convert absolute root input (under project base) to a project-relative path for the generator
        val rootName = try {
            val inPath = Paths.get(rootInput)
            if (inPath.isAbsolute) {
                val normBase = Paths.get(basePath).toAbsolutePath().normalize()
                val normIn = inPath.toAbsolutePath().normalize()
                if (normIn.startsWith(normBase)) normBase.relativize(normIn).toString() else rootInput
            } else rootInput
        } catch (t: Throwable) { rootInput }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val resultMsg = FeatureModulesGenerator(project).generate(
                    basePath,
                    rootName,
                    featureName,
                    includePresentation,
                    includeDatasource,
                    dsCombined,
                    dsRemote,
                    dsLocal,
                    includeDi,
                    orgSegment
                )
                // Append post-generation instructions about required plugin aliases in the project's version catalog
                val extra = """
                
                Post-generation notes:
                
                Please ensure your project declares the plugin aliases used by the generated convention plugins in your version catalog (gradle/libs.versions.toml) and that the plugin aliases are available to the included build. Also add the following plugin aliases to your project plugin usage where appropriate (for example in settings.gradle.kts or root build files) with apply false:
                
                alias(libs.plugins.ksp) apply false
                alias(libs.plugins.hilt) apply false
                
                In your version catalog (gradle/libs.versions.toml) you should define the corresponding plugin entries under [plugins], e.g. add aliases for ksp and hilt that point to the correct plugin IDs and versions for your project.
                
                """.trimIndent()

                Messages.showInfoMessage(project, resultMsg + extra, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate modules: ${t.message}", "Error")
            }
        }
    }
}
