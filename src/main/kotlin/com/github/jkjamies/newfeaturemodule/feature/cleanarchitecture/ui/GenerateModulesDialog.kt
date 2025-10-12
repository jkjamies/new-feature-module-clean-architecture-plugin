package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Simple dialog to collect root folder and feature name. */
class GenerateModulesDialog(project: Project) : DialogWrapper(project) {
    private val rootField = JBTextField("features")
    private val featureField = JBTextField()

    init {
        title = "Generate Clean Architecture Modules"
        // Widen input boxes a bit by increasing preferred column count
        rootField.columns = 28
        featureField.columns = 28
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(12)

        val form = JPanel(GridBagLayout())
        val gc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(8, 12)
        }

        // Root label
        gc.gridx = 0; gc.gridy = 0
        form.add(JBLabel("Root folder under project (e.g., features):"), gc)
        // Root input
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        form.add(rootField, gc)

        // Feature label
        gc.gridx = 0; gc.gridy = 1
        gc.weightx = 0.0
        gc.fill = GridBagConstraints.NONE
        form.add(JBLabel("Feature name (e.g., payments):"), gc)
        // Feature input
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        form.add(featureField, gc)

        panel.add(form, BorderLayout.NORTH)
        return panel
    }

    fun getValues(): Pair<String, String> = rootField.text.trim() to featureField.text.trim()
}
