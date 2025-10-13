package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.actions

import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding.FeatureModulesGenerator
import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui.GenerateModulesDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

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

        val (rootName, featureName) = dialog.getValues()
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
                    includeDi
                )
                Messages.showInfoMessage(project, resultMsg, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate modules: ${t.message}", "Error")
            }
        }
    }
}
