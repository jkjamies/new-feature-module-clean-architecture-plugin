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

/**
 * Dialog used by the action to capture the root features folder and the feature name.
 *
 * Instances are created with a [Project] and presented via [DialogWrapper.showAndGet].
 * The text fields are simple [JBTextField] components sized for typical names.
 */
class GenerateModulesDialog(project: Project) : DialogWrapper(project) {
    private val rootField = JBTextField()
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
        rootField.columns = 28
        featureField.columns = 28
        orgCenterField.columns = 16

        // Update the right-side package preview label when root/feature change
        fun updateOrgPreview() {
            val root = rootField.text.trim().ifEmpty { "root" }
            val feature = featureField.text.trim().ifEmpty { "feature" }
            orgRightLabel.text = ".$root.$feature.{data, di, domain, presentation, dataSource, remoteDataSource, localDataSource}"
        }
        // initial preview
        updateOrgPreview()
        rootField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateOrgPreview()
            }
        })
        featureField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateOrgPreview()
            }
        })

        // Wire up datasource selection logic
        fun updateDatasourceStates() {
            if (!includeDatasourceCheckBox.isSelected) {
                // When section is disabled, clear all selections and disable all options
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
            // Include datasource is enabled: compute enablement/selection per rules
            if (combinedDatasourceCheckBox.isSelected) {
                // combined selected disables and clears remote/local
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
                // when enabling includeDatasource, default remote+local = true, combined = false
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
                // By default (no combined), allow interaction
                combinedDatasourceCheckBox.isEnabled = true
                remoteDatasourceCheckBox.isEnabled = true
                localDatasourceCheckBox.isEnabled = true

                // If exactly one of remote/local is selected, disable the selected one to avoid ending up with none selected
                val remoteSelected = remoteDatasourceCheckBox.isSelected
                val localSelected = localDatasourceCheckBox.isSelected
                val exactlyOne = remoteSelected.xor(localSelected)

                remoteDatasourceCheckBox.isEnabled = !exactlyOne || !remoteSelected
                localDatasourceCheckBox.isEnabled = !exactlyOne || !localSelected
            }
        }

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

        // Wire up DI selection logic
        fun updateDiStates() {
            if (!includeDiCheckBox.isSelected) {
                // Clear selections and disable both when DI not included
                adjustingDiSelections = true
                try {
                    // Explicitly clear both before clearing the group to avoid any retained selection state
                    diHiltRadioButton.isSelected = false
                    diKoinRadioButton.isSelected = false
                    diButtonGroup.clearSelection()
                } finally {
                    adjustingDiSelections = false
                }
                diHiltRadioButton.isEnabled = false
                diKoinRadioButton.isEnabled = false
                // Hide and clear Koin annotations when DI is disabled
                koinAnnotationsCheckBox.isSelected = false
                koinAnnotationsCheckBox.isEnabled = false
                koinAnnotationsCheckBox.isVisible = false
                return
            }

            val isKmp = platformKmpRadioButton.isSelected

            if (isKmp) {
                // On KMP platform, only Koin is allowed
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
                // Android platform: if neither selected, default to Hilt selection explicitly
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
                // With radio buttons, mutual exclusivity is handled by ButtonGroup; both remain enabled
                diHiltRadioButton.isEnabled = true
                diKoinRadioButton.isEnabled = true
            }

            // Koin Annotations checkbox is only relevant when Koin is selected
            val koinSelected = diKoinRadioButton.isSelected
            koinAnnotationsCheckBox.isVisible = koinSelected
            koinAnnotationsCheckBox.isEnabled = koinSelected
            if (!koinSelected) {
                koinAnnotationsCheckBox.isSelected = false
            }
        }

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
            add(orgRightLabel, BorderLayout.EAST)
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
}
