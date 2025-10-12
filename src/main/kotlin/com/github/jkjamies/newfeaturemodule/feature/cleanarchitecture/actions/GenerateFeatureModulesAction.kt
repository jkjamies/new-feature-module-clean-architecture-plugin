package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.actions

import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding.FeatureModulesGenerator
import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui.GenerateModulesDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * Tools -> Generate Clean Architecture Modules
 * Delegates actual work to generator components for clarity and growth.
 */
class GenerateFeatureModulesAction : AnAction("Generate Clean Architecture Modules") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dialog = GenerateModulesDialog(project)
        if (!dialog.showAndGet()) return

        val (rootName, featureName) = dialog.getValues()
        val basePath = project.basePath ?: run {
            Messages.showErrorDialog(project, "Project base path not found", "Error")
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val resultMsg = FeatureModulesGenerator(project).generate(basePath, rootName, featureName)
                Messages.showInfoMessage(project, resultMsg, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate modules: ${t.message}", "Error")
            }
        }
    }
}
