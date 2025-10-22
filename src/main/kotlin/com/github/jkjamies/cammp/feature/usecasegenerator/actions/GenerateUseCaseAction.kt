package com.github.jkjamies.cammp.feature.usecasegenerator.actions

import com.github.jkjamies.cammp.feature.usecasegenerator.scaffolding.UseCaseGenerator
import com.github.jkjamies.cammp.feature.usecasegenerator.ui.GenerateUseCaseDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Paths

class GenerateUseCaseAction : AnAction("Generate UseCase") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dialog = GenerateUseCaseDialog(project)
        if (!dialog.showAndGet()) return

        val useCaseName = dialog.getUseCaseName()
        val targetDir = dialog.getTargetDir() // expected to be the domain module directory
        val diEnabled = dialog.isDiEnabled()
        val useHilt = dialog.isHiltSelected()
        val koinAnnotations = dialog.isKoinAnnotationsSelected()

        val basePath = project.basePath ?: run {
            Messages.showErrorDialog(project, "Project base path not found", "Error")
            return
        }

        // Normalize to project-relative path if possible
        val targetRelative = try {
            val p = Paths.get(targetDir)
            if (p.isAbsolute) {
                val base = Paths.get(basePath).toAbsolutePath().normalize()
                val abs = p.toAbsolutePath().normalize()
                if (abs.startsWith(base)) base.relativize(abs).toString() else targetDir
            } else targetDir
        } catch (t: Throwable) { targetDir }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val result = UseCaseGenerator(project).generate(
                    projectBasePath = basePath,
                    domainModuleDirRelativeToProject = targetRelative,
                    useCaseName = useCaseName,
                    diEnabled = diEnabled,
                    useHilt = useHilt,
                    koinAnnotations = koinAnnotations
                )
                Messages.showInfoMessage(project, result, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate use case: ${t.message}", "Error")
            }
        }
    }
}

