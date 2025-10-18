package com.github.jkjamies.cammp.feature.presentationgenerator.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JSeparator
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Paths
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/**
 * Dialog to gather inputs for generating a Presentation Screen.
 */
class GenerateScreenDialog(private val project: Project) : DialogWrapper(project) {
    private val dirField = JBTextField()
    private val chooseButton = JButton("Choose...")
    private val nameField = JBTextField()

    private val cbFlow = JBCheckBox("Use Flow StateHolder")
    private val cbScreen = JBCheckBox("Use Screen StateHolder")
    private val cbNavigation = JBCheckBox("Add Navigation Destination (Navigation Compose)")

    // UI Architecture pattern selection (placed before DI section)
    private val rbMvvm = JBRadioButton("MVVM", false)
    private val rbMvi = JBRadioButton("MVI", true)
    private val patternGroup = ButtonGroup().apply {
        add(rbMvvm)
        add(rbMvi)
    }

    private val cbEnableDi = JBCheckBox("Enable Dependency Injection", true)
    private val rbHilt = JBRadioButton("Hilt", true)
    private val rbKoin = JBRadioButton("Koin")
    private val cbKoinAnnotations = JBCheckBox("Koin Annotations")

    // Track whether the user has interacted to control when to show validation errors
    private var userInteracted: Boolean = false

    init {
        title = "Generate Presentation Screen"
        isResizable = true
        init()

        // Default the directory field to the current project so chooser opens there
        project.basePath?.let { dirField.text = it }

        chooseButton.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            // Limit browsing to the current project root, to match the modules generator
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

        // Live validation on directory field changes
        dirField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                userInteracted = true
                updateOkAndError()
            }
        })

        // Live validation on screen name changes
        nameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                userInteracted = true
                updateOkAndError()
            }
        })

        val group = ButtonGroup().apply {
            add(rbHilt)
            add(rbKoin)
        }
        // Toggle DI options visibility/enabled states
        cbEnableDi.addItemListener { updateDiControls() }
        rbKoin.addItemListener { updateDiControls() }
        rbHilt.addItemListener { updateDiControls() }
        // Initialize DI controls state
        updateDiControls()

        // Initialize validation state immediately (OK disabled if invalid; no error text until user interacts)
        updateOkAndError()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        // Make the dialog wider (approximately 2–3x)
        panel.preferredSize = JBUI.size(900, 400)
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
            c.gridx = 0; c.weightx = 0.0
            form.add(JBLabel(label), c)
            c.gridx = 1; c.weightx = 1.0
            form.add(comp, c)
            c.gridy += 1
        }

        val dirPanel = JPanel(BorderLayout()).apply {
            add(dirField, BorderLayout.CENTER)
            add(chooseButton, BorderLayout.EAST)
        }
        row("Directory (desired presentation module):", dirPanel)
        row("Screen name (e.g., HomeScreen):", nameField)

        // Options
        c.gridx = 0; c.gridwidth = 2
        form.add(cbFlow, c); c.gridy += 1
        form.add(cbScreen, c); c.gridy += 1
        form.add(cbNavigation, c); c.gridy += 1

        // UI Architecture Pattern title + options below (full width), order: MVI then MVVM
        c.gridx = 0; c.gridwidth = 2
        form.add(JBLabel("UI Architecture Pattern:"), c); c.gridy += 1
        val patternOptions = JBPanel<JBPanel<*>>()
        patternOptions.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
        patternOptions.add(rbMvi)
        patternOptions.add(rbMvvm)
        c.gridx = 0; c.gridwidth = 2
        form.add(patternOptions, c); c.gridy += 1

        // Dependency Injection title + options below (full width)
        c.gridx = 0; c.gridwidth = 2
        form.add(JBLabel("Dependency Injection:"), c); c.gridy += 1
        val diEnablePanel = JBPanel<JBPanel<*>>()
        diEnablePanel.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
        diEnablePanel.add(cbEnableDi)
        c.gridx = 0; c.gridwidth = 2
        form.add(diEnablePanel, c); c.gridy += 1
        val diRow = JBPanel<JBPanel<*>>()
        diRow.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
        diRow.add(rbHilt)
        diRow.add(rbKoin)
        diRow.add(cbKoinAnnotations)
        c.gridx = 0; c.gridwidth = 2
        form.add(diRow, c); c.gridy += 1

        panel.add(form, BorderLayout.CENTER)
        return panel
    }

    private fun updateDiControls() {
        val enabled = cbEnableDi.isSelected
        rbHilt.isEnabled = enabled
        rbKoin.isEnabled = enabled
        val showKoinAnn = enabled && rbKoin.isSelected
        cbKoinAnnotations.isEnabled = showKoinAnn
        cbKoinAnnotations.isVisible = showKoinAnn
        cbKoinAnnotations.revalidate()
        cbKoinAnnotations.repaint()
    }

    // Live validation helper: disable OK when invalid; show error only after interaction
    private fun updateOkAndError() {
        val vi = doValidate()
        setOKActionEnabled(vi == null)
        // Only show error text after the user interacts (types or chooses)
        if (userInteracted) {
            setErrorText(vi?.message, vi?.component)
        } else {
            setErrorText(null)
        }
    }

    override fun doValidate(): ValidationInfo? {
        val dir = dirField.text.trim()
        if (dir.isEmpty()) return ValidationInfo("Please choose a directory", dirField)
        // Require selecting a presentation module directory
        run {
            val last = try {
                java.nio.file.Paths.get(dir).fileName?.toString()
            } catch (t: Throwable) {
                null
            }
            if (!"presentation".equals(last, ignoreCase = true)) {
                return ValidationInfo("You must select a presentation module directory", dirField)
            }
        }
        val name = nameField.text.trim()
        if (name.isEmpty()) return ValidationInfo("Please enter a screen name", nameField)
        if (!name.first().isLetter()) return ValidationInfo("Screen name should start with a letter", nameField)
        return null
    }

    fun getTargetDir(): String = dirField.text.trim()
    fun getScreenName(): String = nameField.text.trim()

    fun isNavigationSelected(): Boolean = cbNavigation.isSelected
    fun isFlowStateHolderSelected(): Boolean = cbFlow.isSelected
    fun isScreenStateHolderSelected(): Boolean = cbScreen.isSelected

    enum class DiChoice { HILT, KOIN }
    fun getDiChoice(): DiChoice = if (rbKoin.isSelected) DiChoice.KOIN else DiChoice.HILT
    fun isKoinAnnotationsSelected(): Boolean = cbKoinAnnotations.isSelected

    enum class PatternChoice { MVI, MVVM }
    fun getPatternChoice(): PatternChoice = if (rbMvvm.isSelected) PatternChoice.MVVM else PatternChoice.MVI
}
