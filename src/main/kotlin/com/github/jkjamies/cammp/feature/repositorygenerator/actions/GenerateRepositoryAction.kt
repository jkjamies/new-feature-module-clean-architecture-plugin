package com.github.jkjamies.cammp.feature.repositorygenerator.actions

import com.github.jkjamies.cammp.feature.repositorygenerator.scaffolding.RepositoryGenerator
import com.github.jkjamies.cammp.feature.repositorygenerator.ui.GenerateRepositoryDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Paths

/**
 * Action entry point that opens [GenerateRepositoryDialog] and scaffolds repository interfaces/implementations
 * across domain and data modules.
 */
class GenerateRepositoryAction : AnAction("Generate Repository") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dialog = GenerateRepositoryDialog(project)
        if (!dialog.showAndGet()) return

        val repoName = dialog.getRepositoryName()
        val targetDir = dialog.getTargetDir() // expected to be the data module directory
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
                val result = RepositoryGenerator(project).generate(
                    projectBasePath = basePath,
                    dataModuleDirRelativeToProject = targetRelative,
                    repositoryName = repoName,
                    diEnabled = diEnabled,
                    useHilt = useHilt,
                    koinAnnotations = koinAnnotations
                )
                Messages.showInfoMessage(project, result, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate repository: ${t.message}", "Error")
            }
        }
    }
}
