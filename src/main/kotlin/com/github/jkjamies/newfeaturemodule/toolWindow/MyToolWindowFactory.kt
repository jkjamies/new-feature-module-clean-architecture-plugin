package com.github.jkjamies.newfeaturemodule.toolWindow

import com.github.jkjamies.newfeaturemodule.MyBundle
import com.github.jkjamies.newfeaturemodule.services.MyApplicationService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.ide.DataManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
        if (toolWindow is ToolWindowEx) {
            toolWindow.setTitle("Feature Modules")
        }
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(private val toolWindow: ToolWindow) {

        private val appService = ApplicationManager.getApplication().getService(MyApplicationService::class.java)

        fun getContent(): JPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            // Top area label from template (left as-is)
            add(label, BorderLayout.NORTH)

            // Primary action button to trigger module generation
            val generateButton = JButton("Generate Modules")
            generateButton.addActionListener {
                val actionId = "com.github.jkjamies.newfeaturemodule.actions.GenerateModulesAction"
                val action = ActionManager.getInstance().getAction(actionId)
                if (action != null) {
                    // Ensure the DataContext contains the current Project; otherwise e.project will be null and action will no-op
                    val dataContext: DataContext = com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(toolWindow.project)
                    val presentation: Presentation = action.templatePresentation.clone()
                    val event = AnActionEvent.createFromDataContext(ActionPlaces.TOOLWINDOW_CONTENT, presentation, dataContext)
                    action.actionPerformed(event)
                } else {
                    label.text = "Action not found: $actionId"
                }
            }
            add(generateButton, BorderLayout.CENTER)

            // Keep the sample shuffle button to demonstrate state changes
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", appService.getRandomNumber())
                }
            }, BorderLayout.SOUTH)
        }
    }
}
