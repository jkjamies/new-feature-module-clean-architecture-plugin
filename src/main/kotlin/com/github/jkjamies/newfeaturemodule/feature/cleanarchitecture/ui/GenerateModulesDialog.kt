package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import com.intellij.openapi.ui.ValidationInfo
import java.nio.file.Paths

/**
 * Dialog used by the action to capture the root features folder and the feature name.
 *
 * Instances are created with a [Project] and presented via [DialogWrapper.showAndGet].
 * The text fields are simple [JBTextField] components sized for typical names.
 */
class GenerateModulesDialog(project: Project) : DialogWrapper(project) {
    private val projectBasePath: String? = project.basePath
    private val rootField = com.intellij.openapi.ui.TextFieldWithBrowseButton(JBTextField())
    private val featureField = JBTextField()
    // Organization segmented UI: left label "com.", center input, right dynamic label
    private val orgLeftLabel = JBLabel("com.")
    private val orgCenterField = JBTextField("jkjamies")
    private val orgRightLabel = JBLabel("")

    // Platform section controls
    private val platformAndroidRadioButton = JBRadioButton("Android", true)
    private val platformKmpRadioButton = JBRadioButton("Kotlin Multiplatform (KMP)", false)
    private val platformButtonGroup = ButtonGroup().apply {
        add(platformAndroidRadioButton)
        add(platformKmpRadioButton)
    }
    private val platformOptionsPanel = JPanel().apply {
        add(platformAndroidRadioButton)
        add(platformKmpRadioButton)
        isVisible = true
    }

    // Presentation section controls
    private val includePresentationCheckBox = JBCheckBox("Include presentation module", true)

    // Data section controls
    private val includeDatasourceCheckBox = JBCheckBox("Include datasource", false)
    private val combinedDatasourceCheckBox = JBCheckBox("Combined datasource", false)
    private val remoteDatasourceCheckBox = JBCheckBox("Remote datasource", false)
    private val localDatasourceCheckBox = JBCheckBox("Local datasource", false)
    private val datasourceOptionsPanel = JPanel().apply {
        // simple horizontal panel to hold datasource options (Remote, Local, then Combined)
        add(remoteDatasourceCheckBox)
        add(localDatasourceCheckBox)
        add(combinedDatasourceCheckBox)
        // Always visible; suboptions are disabled when 'Include datasource' is not checked
        isVisible = true
    }

    // Dependency Injection section controls
    private val includeDiCheckBox = JBCheckBox("Include dependency injection module", true)
    private val diHiltRadioButton = JBRadioButton("Hilt", true)
    private val diKoinRadioButton = JBRadioButton("Koin", false)
    private val diButtonGroup = ButtonGroup().apply {
        add(diHiltRadioButton)
        add(diKoinRadioButton)
    }
    private val koinAnnotationsCheckBox = JBCheckBox("Koin Annotations", false)
    private val diOptionsPanel = JPanel().apply {
        add(diHiltRadioButton)
        add(diKoinRadioButton)
        add(koinAnnotationsCheckBox)
        isVisible = true
    }

    private var adjustingDatasourceSelections = false
    private var adjustingDiSelections = false

    init {
        title = "Generate Clean Architecture Modules"
        rootField.textField.columns = 28
        featureField.columns = 28
        orgCenterField.columns = 16

        // Prepopulate org segment with the last path segment of the project root
        projectBasePath?.let { base ->
            val seg = try { Paths.get(base).fileName?.toString() ?: "" } catch (t: Throwable) { "" }
            if (seg.isNotBlank()) orgCenterField.text = seg
        }

        // Update the right-side package preview label when root/feature change
        // Moved to private method for readability
        // Configure directory chooser scoped to project base
        run {
            val descriptor = com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val basePath = projectBasePath
            if (basePath != null) {
                val baseVf = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
                if (baseVf != null) descriptor.withRoots(baseVf)
            }
            rootField.addBrowseFolderListener(
                "Select root folder under project",
                null,
                project,
                descriptor,
                object : com.intellij.openapi.ui.TextComponentAccessor<javax.swing.JTextField> {
                    override fun getText(component: javax.swing.JTextField): String {
                        val t = component.text
                        val base = projectBasePath
                        return if (t.isNullOrBlank() && base != null) base else t
                    }
                    override fun setText(component: javax.swing.JTextField, text: String) {
                        component.text = text
                    }
                }
            )
        }

        // Prepopulate root field with project base path so the user sees the full absolute path
        projectBasePath?.let { base -> rootField.text = base }

        updateOrgPreview()
        rootField.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                // Keep user input as-is (absolute or relative). Only update the preview label.
                updateOrgPreview()
            }
        })
        featureField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateOrgPreview()
            }
        })

        // Wire up datasource selection logic
        // Moved state update function to a private method for readability

        includeDatasourceCheckBox.addItemListener { updateDatasourceStates() }
        combinedDatasourceCheckBox.addItemListener { e ->
            if (adjustingDatasourceSelections) return@addItemListener
            val selected = e.stateChange == ItemEvent.SELECTED
            if (selected) {
                // when combined is selected, other two cannot be selected
                adjustingDatasourceSelections = true
                try {
                    remoteDatasourceCheckBox.isSelected = false
                    localDatasourceCheckBox.isSelected = false
                } finally {
                    adjustingDatasourceSelections = false
                }
            }
            updateDatasourceStates()
        }
        val remoteLocalListener = java.awt.event.ItemListener {
            if (adjustingDatasourceSelections) return@ItemListener
            val anySelected = remoteDatasourceCheckBox.isSelected || localDatasourceCheckBox.isSelected
            if (anySelected) {
                // selecting either remote or local unselects combined
                adjustingDatasourceSelections = true
                try {
                    combinedDatasourceCheckBox.isSelected = false
                } finally {
                    adjustingDatasourceSelections = false
                }
            }
            updateDatasourceStates()
        }
        remoteDatasourceCheckBox.addItemListener(remoteLocalListener)
        localDatasourceCheckBox.addItemListener(remoteLocalListener)


        includeDiCheckBox.addItemListener { updateDiStates() }
        diHiltRadioButton.addItemListener { e ->
            if (adjustingDiSelections) return@addItemListener
            if (e.stateChange == ItemEvent.SELECTED) updateDiStates()
        }
        diKoinRadioButton.addItemListener { e ->
            if (adjustingDiSelections) return@addItemListener
            if (e.stateChange == ItemEvent.SELECTED) updateDiStates()
        }
        platformAndroidRadioButton.addItemListener { e -> if (e.stateChange == ItemEvent.SELECTED) updateDiStates() }
        platformKmpRadioButton.addItemListener { e -> if (e.stateChange == ItemEvent.SELECTED) updateDiStates() }

        // initialize visibility/enabled states on dialog creation
        updateDatasourceStates()
        updateDiStates()

        init()
        initValidation()
    }

    override fun doValidate(): ValidationInfo? {
        val base = projectBasePath ?: return null
        val input = rootField.text.trim()
        if (input.isBlank()) return null
        // If input is absolute and not under base, return error
        val normBase = Paths.get(base).normalize().toAbsolutePath()
        return try {
            val inPath = Paths.get(input).normalize().toAbsolutePath()
            val isAbsolute = inPath.isAbsolute
            val underBase = inPath.startsWith(normBase)
            if (isAbsolute && !underBase) {
                ValidationInfo("Path must be under the project root: ${normBase}", rootField)
            } else null
        } catch (t: Throwable) {
            // If it's not a valid path string, let it pass as plain relative text
            null
        }
    }

    /**
     * Builds the form content for the dialog using a small grid layout.
     *
     * @return a Swing [JComponent] containing the inputs
     */
    protected override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(12)

        val form = JPanel(GridBagLayout())
        val gc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(8, 12)
        }

        // Root folder input
        gc.gridx = 0; gc.gridy = 0
        form.add(JBLabel("Root folder under project (e.g., features):"), gc)
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        form.add(rootField, gc)

        // Feature name input
        gc.gridx = 0; gc.gridy = 1
        gc.weightx = 0.0
        gc.fill = GridBagConstraints.NONE
        form.add(JBLabel("Feature name (e.g., payments):"), gc)
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        form.add(featureField, gc)

        // Organization segmented input (left.center.right)
        gc.gridx = 0; gc.gridy = 2
        form.add(JBLabel("Organization segments (left.center.right):"), gc)
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        val orgPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyLeft(0)
            add(orgLeftLabel, BorderLayout.WEST)
            add(orgCenterField, BorderLayout.CENTER)
            // Wrap right preview label into a fixed-width, left-aligned container to avoid layout jitter while typing
            val maxSample = ".this_is_a_sample_root_name.this_is_a_sample_feature_name.{data, di, domain, presentation, dataSource, remoteDataSource, localDataSource}"
            val fm = orgRightLabel.getFontMetrics(orgRightLabel.font)
            val w = fm.stringWidth(maxSample) + JBUI.scale(8)
            val h = orgRightLabel.preferredSize.height
            orgRightLabel.horizontalAlignment = javax.swing.SwingConstants.LEFT
            val rightPreviewContainer = JPanel(BorderLayout()).apply {
                preferredSize = java.awt.Dimension(w, h)
                minimumSize = java.awt.Dimension(w, h)
                add(orgRightLabel, BorderLayout.CENTER)
            }
            add(rightPreviewContainer, BorderLayout.EAST)
        }
        form.add(orgPanel, gc)

        // Platform section label
        gc.gridx = 0; gc.gridy = 3
        form.add(JBLabel("Platform:"), gc)
        // Platform options panel (Android / KMP)
        gc.gridx = 1; gc.gridy = 3
        form.add(platformOptionsPanel, gc)

        // Data section label
        gc.gridx = 0; gc.gridy = 4
        form.add(JBLabel("Data:"), gc)
        // Include datasource checkbox
        gc.gridx = 1; gc.gridy = 4
        form.add(includeDatasourceCheckBox, gc)

        // Datasource options panel (always visible; disabled when Include datasource is off)
        gc.gridx = 1; gc.gridy = 5
        form.add(datasourceOptionsPanel, gc)

        // Dependency Injection section label
        gc.gridx = 0; gc.gridy = 6
        form.add(JBLabel("Dependency Injection:"), gc)
        // Include DI checkbox
        gc.gridx = 1; gc.gridy = 6
        form.add(includeDiCheckBox, gc)
        // DI options panel (always visible; disabled when include DI is off)
        gc.gridx = 1; gc.gridy = 7
        form.add(diOptionsPanel, gc)

        // Presentation section label
        gc.gridx = 0; gc.gridy = 8
        form.add(JBLabel("Presentation:"), gc)
        // Include presentation checkbox
        gc.gridx = 1; gc.gridy = 8
        form.add(includePresentationCheckBox, gc)

        panel.add(form, BorderLayout.NORTH)
        return panel
    }

    /**
     * Returns the trimmed root and feature values as a [Pair].
     */
    fun getValues(): Pair<String, String> = rootField.text.trim() to featureField.text.trim()

    /** Returns the organization segment inserted after 'com.' in the base package. */
    fun getOrgSegment(): String {
        return orgCenterField.text.trim()
    }

    /** Returns whether the presentation module should be included. */
    fun getIncludePresentation(): Boolean = includePresentationCheckBox.isSelected

    /** Data section getters */
    fun getIncludeDatasource(): Boolean = includeDatasourceCheckBox.isSelected
    fun isDatasourceCombinedSelected(): Boolean = combinedDatasourceCheckBox.isSelected
    fun isDatasourceRemoteSelected(): Boolean = remoteDatasourceCheckBox.isSelected
    fun isDatasourceLocalSelected(): Boolean = localDatasourceCheckBox.isSelected

    /** DI section getters */
    fun getIncludeDi(): Boolean = includeDiCheckBox.isSelected
    fun isDiHiltSelected(): Boolean = diHiltRadioButton.isSelected
    fun isDiKoinSelected(): Boolean = diKoinRadioButton.isSelected
    fun isDiKoinAnnotationsSelected(): Boolean = koinAnnotationsCheckBox.isSelected

    // ---- Extracted helpers for readability ----
    private fun updateOrgPreview() {
        val rawRoot = rootField.text.trim()
        val displayRoot = try {
            if (rawRoot.isBlank()) "root" else Paths.get(rawRoot).fileName?.toString() ?: rawRoot
        } catch (t: Throwable) {
            if (rawRoot.isBlank()) "root" else rawRoot
        }
        val feature = featureField.text.trim().ifEmpty { "feature" }
        val base = projectBasePath
        val omitRoot = try {
            if (base.isNullOrBlank()) false else {
                val normIn = Paths.get(rawRoot).toAbsolutePath().normalize()
                val normBase = Paths.get(base).toAbsolutePath().normalize()
                normIn == normBase
            }
        } catch (t: Throwable) { false }
        val suffix = if (omitRoot) {
            ".${feature}.{data, di, domain, presentation, dataSource, remoteDataSource, localDataSource}"
        } else {
            ".${displayRoot}.${feature}.{data, di, domain, presentation, dataSource, remoteDataSource, localDataSource}"
        }
        orgRightLabel.text = suffix
    }

    private fun updateDatasourceStates() {
        if (!includeDatasourceCheckBox.isSelected) {
            adjustingDatasourceSelections = true
            try {
                combinedDatasourceCheckBox.isSelected = false
                remoteDatasourceCheckBox.isSelected = false
                localDatasourceCheckBox.isSelected = false
            } finally {
                adjustingDatasourceSelections = false
            }
            combinedDatasourceCheckBox.isEnabled = false
            remoteDatasourceCheckBox.isEnabled = false
            localDatasourceCheckBox.isEnabled = false
            return
        }
        if (combinedDatasourceCheckBox.isSelected) {
            adjustingDatasourceSelections = true
            try {
                remoteDatasourceCheckBox.isSelected = false
                localDatasourceCheckBox.isSelected = false
            } finally {
                adjustingDatasourceSelections = false
            }
            remoteDatasourceCheckBox.isEnabled = false
            localDatasourceCheckBox.isEnabled = false
            combinedDatasourceCheckBox.isEnabled = true
        } else {
            if (includeDatasourceCheckBox.isSelected) {
                adjustingDatasourceSelections = true
                try {
                    if (!remoteDatasourceCheckBox.isSelected && !localDatasourceCheckBox.isSelected && !combinedDatasourceCheckBox.isSelected) {
                        remoteDatasourceCheckBox.isSelected = true
                        localDatasourceCheckBox.isSelected = true
                    }
                    combinedDatasourceCheckBox.isSelected = false
                } finally {
                    adjustingDatasourceSelections = false
                }
            }
            combinedDatasourceCheckBox.isEnabled = true
            remoteDatasourceCheckBox.isEnabled = true
            localDatasourceCheckBox.isEnabled = true

            val remoteSelected = remoteDatasourceCheckBox.isSelected
            val localSelected = localDatasourceCheckBox.isSelected
            val exactlyOne = remoteSelected.xor(localSelected)

            remoteDatasourceCheckBox.isEnabled = !exactlyOne || !remoteSelected
            localDatasourceCheckBox.isEnabled = !exactlyOne || !localSelected
        }
    }

    private fun updateDiStates() {
        if (!includeDiCheckBox.isSelected) {
            adjustingDiSelections = true
            try {
                diHiltRadioButton.isSelected = false
                diKoinRadioButton.isSelected = false
                diButtonGroup.clearSelection()
            } finally {
                adjustingDiSelections = false
            }
            diHiltRadioButton.isEnabled = false
            diKoinRadioButton.isEnabled = false
            koinAnnotationsCheckBox.isSelected = false
            koinAnnotationsCheckBox.isEnabled = false
            koinAnnotationsCheckBox.isVisible = false
            return
        }

        val isKmp = platformKmpRadioButton.isSelected

        if (isKmp) {
            adjustingDiSelections = true
            try {
                diButtonGroup.clearSelection()
                diKoinRadioButton.isSelected = true
                diHiltRadioButton.isSelected = false
            } finally {
                adjustingDiSelections = false
            }
            diHiltRadioButton.isEnabled = false
            diKoinRadioButton.isEnabled = true
        } else {
            if (!diHiltRadioButton.isSelected && !diKoinRadioButton.isSelected) {
                adjustingDiSelections = true
                try {
                    diButtonGroup.clearSelection()
                    diHiltRadioButton.isSelected = true
                    diKoinRadioButton.isSelected = false
                } finally {
                    adjustingDiSelections = false
                }
            }
            diHiltRadioButton.isEnabled = true
            diKoinRadioButton.isEnabled = true
        }

        val koinSelected = diKoinRadioButton.isSelected
        koinAnnotationsCheckBox.isVisible = koinSelected
        koinAnnotationsCheckBox.isEnabled = koinSelected
        if (!koinSelected) {
            koinAnnotationsCheckBox.isSelected = false
        }
    }
}

