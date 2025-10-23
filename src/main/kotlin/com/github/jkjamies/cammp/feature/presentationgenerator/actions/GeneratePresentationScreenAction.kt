package com.github.jkjamies.cammp.feature.presentationgenerator.actions

import com.github.jkjamies.cammp.feature.presentationgenerator.scaffolding.PresentationScreenGenerator
import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.nio.file.Paths

/**
 * Action entry point that opens [GenerateScreenDialog] and scaffolds a Presentation Screen.
 */
class GeneratePresentationScreenAction : AnAction("Generate Presentation Screen") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dialog = GenerateScreenDialog(project)
        if (!dialog.showAndGet()) return

        val targetDir = dialog.getTargetDir()
        val screenName = dialog.getScreenName()
        val addNavigation = dialog.isNavigationSelected()
        val useFlowStateHolder = dialog.isFlowStateHolderSelected()
        val useScreenStateHolder = dialog.isScreenStateHolderSelected()
        val diChoice = dialog.getDiChoice()
        val koinAnnotations = dialog.isKoinAnnotationsSelected()
        val patternChoice = dialog.getPatternChoice()
        val selectedUseCaseModulePaths = dialog.getSelectedUseCaseGradlePaths()
        val selectedUseCaseFqns = dialog.getSelectedUseCaseFqns()

        val basePath = project.basePath ?: run {
            Messages.showErrorDialog(project, "Project base path not found", "Error")
            return
        }

        // Convert absolute to project-relative when under project
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
                val result = PresentationScreenGenerator(project).generate(
                    projectBasePath = basePath,
                    targetDirRelativeToProject = targetRelative,
                    screenName = screenName,
                    addNavigation = addNavigation,
                    useFlowStateHolder = useFlowStateHolder,
                    useScreenStateHolder = useScreenStateHolder,
                    diChoice = diChoice,
                    koinAnnotations = koinAnnotations,
                    patternChoice = patternChoice,
                    selectedUseCaseFqns = selectedUseCaseFqns,
                    selectedUseCaseModulePaths = selectedUseCaseModulePaths
                )
                Messages.showInfoMessage(project, result, "Success")
            } catch (t: Throwable) {
                Messages.showErrorDialog(project, "Failed to generate screen: ${t.message}", "Error")
            }
        }
    }
}
