package com.github.jkjamies.cammp.feature.usecasegenerator.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Paths
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class GenerateUseCaseDialog(private val project: Project) : DialogWrapper(project) {
    private val dirField = JBTextField()
    private val chooseButton = JButton("Choose...")
    private val nameField = JBTextField()

    // DI controls (same UI as others)
    private val cbEnableDi = JBCheckBox("Enable Dependency Injection", true)
    private val rbHilt = JBRadioButton("Hilt", true)
    private val rbKoin = JBRadioButton("Koin")
    private val cbKoinAnnotations = JBCheckBox("Koin Annotations")

    private var userInteracted: Boolean = false

    init {
        title = "Generate Use Case"
        isResizable = true
        init()

        project.basePath?.let { dirField.text = it }

        chooseButton.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val basePath = project.basePath
            if (basePath != null) {
                val baseVf = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
                if (baseVf != null) descriptor.withRoots(baseVf)
            }
            val currentText = dirField.text
            val toSelectPath = if (currentText.isNullOrBlank()) basePath else currentText
            val toSelect = if (toSelectPath != null) VfsUtil.findFile(Paths.get(toSelectPath).normalize(), true) else null
            val file = FileChooser.chooseFile(descriptor, project, toSelect)
            if (file != null) {
                dirField.text = file.path
                userInteracted = true
                updateOkAndError()
            }
        }

        dirField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                userInteracted = true
                updateOkAndError()
            }
        })
        nameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                userInteracted = true
                updateOkAndError()
            }
        })

        ButtonGroup().apply {
            add(rbHilt)
            add(rbKoin)
        }
        cbEnableDi.addItemListener { updateDiControls() }
        rbKoin.addItemListener { updateDiControls() }
        rbHilt.addItemListener { updateDiControls() }
        updateDiControls()

        updateOkAndError()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = JBUI.size(780, 280)
        val form = JPanel(GridBagLayout())
        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            gridy = 0
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
        }

        fun row(label: String, comp: JComponent) {
            c.gridx = 0; c.weightx = 0.0; c.gridwidth = 1
            form.add(JBLabel(label), c)
            c.gridx = 1; c.weightx = 1.0; c.gridwidth = 1
            form.add(comp, c)
            c.gridy += 1
        }

        val dirPanel = JPanel(BorderLayout()).apply {
            add(dirField, BorderLayout.CENTER)
            add(chooseButton, BorderLayout.EAST)
        }
        row("Domain module directory:", dirPanel)
        row("Use case name:", nameField)

        // Dependency Injection title + options rows (full width), aligned with other dialogs
        c.gridx = 0; c.gridwidth = 2; c.weightx = 1.0
        form.add(JBLabel("Dependency Injection:"), c); c.gridy += 1

        val diEnablePanel = JPanel().apply {
            layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
            add(cbEnableDi)
        }
        c.gridx = 0; c.gridwidth = 2
        form.add(diEnablePanel, c); c.gridy += 1

        val diRow = JPanel().apply {
            layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
            add(rbHilt)
            add(rbKoin)
            add(cbKoinAnnotations)
        }
        c.gridx = 0; c.gridwidth = 2
        form.add(diRow, c); c.gridy += 1

        panel.add(form, BorderLayout.NORTH)
        return panel
    }

    private fun updateDiControls() {
        val enabled = cbEnableDi.isSelected
        rbHilt.isEnabled = enabled
        rbKoin.isEnabled = enabled
        val showKoinAnn = enabled && rbKoin.isSelected
        cbKoinAnnotations.isEnabled = showKoinAnn
        cbKoinAnnotations.isVisible = showKoinAnn
        if (!showKoinAnn) cbKoinAnnotations.isSelected = false
        cbKoinAnnotations.revalidate()
        cbKoinAnnotations.repaint()
    }

    private fun updateOkAndError() {
        isOKActionEnabled = doValidateAll().isEmpty()
        if (userInteracted) setErrorInfoAll(doValidateAll()) else setErrorInfoAll(emptyList())
    }

    override fun doValidate(): ValidationInfo? = null

    override fun doValidateAll(): List<ValidationInfo> {
        val errors = mutableListOf<ValidationInfo>()
        val dir = dirField.text
        if (dir.isBlank()) {
            errors += ValidationInfo("Please choose a directory", dirField)
        } else {
            val last = dir.trimEnd('/', '\\').substringAfterLast('/')
                .substringAfterLast('\\')
            if (!last.equals("domain", ignoreCase = true)) {
                errors += ValidationInfo("Selected directory must be a domain module", dirField)
            }
        }
        if (nameField.text.isBlank()) {
            errors += ValidationInfo("Please enter a use case name", nameField)
        }
        return errors
    }

    fun getTargetDir(): String = dirField.text
    fun getUseCaseName(): String = nameField.text.trim()
    fun isDiEnabled(): Boolean = cbEnableDi.isSelected
    fun isHiltSelected(): Boolean = rbHilt.isSelected
    fun isKoinAnnotationsSelected(): Boolean = cbKoinAnnotations.isSelected
}

