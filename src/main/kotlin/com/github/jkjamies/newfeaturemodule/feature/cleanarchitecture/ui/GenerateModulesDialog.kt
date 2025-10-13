package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Dialog used by the action to capture the root features folder and the feature name.
 *
 * Instances are created with a [Project] and presented via [DialogWrapper.showAndGet].
 * The text fields are simple [JBTextField] components sized for typical names.
 */
class GenerateModulesDialog(project: Project) : DialogWrapper(project) {
    private val rootField = JBTextField()
    private val featureField = JBTextField()
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
    private val diHiltCheckBox = JBCheckBox("Hilt", true)
    private val diKoinCheckBox = JBCheckBox("Koin", false)
    private val diOptionsPanel = JPanel().apply {
        add(diHiltCheckBox)
        add(diKoinCheckBox)
        isVisible = true
    }

    private var adjustingDatasourceSelections = false
    private var adjustingDiSelections = false

    init {
        title = "Generate Clean Architecture Modules"
        rootField.columns = 28
        featureField.columns = 28

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
                    diHiltCheckBox.isSelected = false
                    diKoinCheckBox.isSelected = false
                } finally {
                    adjustingDiSelections = false
                }
                diHiltCheckBox.isEnabled = false
                diKoinCheckBox.isEnabled = false
                return
            }
            // Ensure single-selection; default to Hilt if neither selected
            if (!diHiltCheckBox.isSelected && !diKoinCheckBox.isSelected) {
                adjustingDiSelections = true
                try {
                    diHiltCheckBox.isSelected = true
                } finally {
                    adjustingDiSelections = false
                }
            }
            // Enforce mutual exclusivity
            if (diHiltCheckBox.isSelected && diKoinCheckBox.isSelected) {
                // prefer the one that triggered, but here just keep Hilt and clear Koin
                adjustingDiSelections = true
                try {
                    diKoinCheckBox.isSelected = false
                } finally {
                    adjustingDiSelections = false
                }
            }
            // When exactly one selected, disable the selected one so user can't unselect the last option
            val hiltSelected = diHiltCheckBox.isSelected
            val koinSelected = diKoinCheckBox.isSelected
            val exactlyOne = hiltSelected.xor(koinSelected)
            diHiltCheckBox.isEnabled = !exactlyOne || !hiltSelected
            diKoinCheckBox.isEnabled = !exactlyOne || !koinSelected
        }

        includeDiCheckBox.addItemListener { updateDiStates() }
        diHiltCheckBox.addItemListener { e ->
            if (adjustingDiSelections) return@addItemListener
            if ((e as ItemEvent).stateChange == ItemEvent.SELECTED) {
                adjustingDiSelections = true
                try {
                    diKoinCheckBox.isSelected = false
                } finally {
                    adjustingDiSelections = false
                }
            }
            updateDiStates()
        }
        diKoinCheckBox.addItemListener { e ->
            if (adjustingDiSelections) return@addItemListener
            if ((e as java.awt.event.ItemEvent).stateChange == ItemEvent.SELECTED) {
                adjustingDiSelections = true
                try {
                    diHiltCheckBox.isSelected = false
                } finally {
                    adjustingDiSelections = false
                }
            }
            updateDiStates()
        }

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

        gc.gridx = 0; gc.gridy = 0
        form.add(JBLabel("Root folder under project (e.g., features):"), gc)
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        form.add(rootField, gc)

        gc.gridx = 0; gc.gridy = 1
        gc.weightx = 0.0
        gc.fill = GridBagConstraints.NONE
        form.add(JBLabel("Feature name (e.g., payments):"), gc)
        gc.gridx = 1
        gc.fill = GridBagConstraints.HORIZONTAL
        gc.weightx = 1.0
        form.add(featureField, gc)

        // Data section label
        gc.gridx = 0; gc.gridy = 2
        form.add(JBLabel("Data:"), gc)
        // Include datasource checkbox
        gc.gridx = 1; gc.gridy = 2
        form.add(includeDatasourceCheckBox, gc)

        // Datasource options panel (always visible; disabled when Include datasource is off)
        gc.gridx = 1; gc.gridy = 3
        form.add(datasourceOptionsPanel, gc)

        // Dependency Injection section label
        gc.gridx = 0; gc.gridy = 4
        form.add(JBLabel("Dependency Injection:"), gc)
        // Include DI checkbox
        gc.gridx = 1; gc.gridy = 4
        form.add(includeDiCheckBox, gc)
        // DI options panel (always visible; disabled when include DI is off)
        gc.gridx = 1; gc.gridy = 5
        form.add(diOptionsPanel, gc)

        // Presentation section label
        gc.gridx = 0; gc.gridy = 6
        form.add(JBLabel("Presentation:"), gc)
        // Include presentation checkbox
        gc.gridx = 1; gc.gridy = 6
        form.add(includePresentationCheckBox, gc)

        panel.add(form, BorderLayout.NORTH)
        return panel
    }

    /**
     * Returns the trimmed root and feature values as a [Pair].
     */
    fun getValues(): Pair<String, String> = rootField.text.trim() to featureField.text.trim()

    /** Returns whether the presentation module should be included. */
    fun getIncludePresentation(): Boolean = includePresentationCheckBox.isSelected

    /** Data section getters */
    fun getIncludeDatasource(): Boolean = includeDatasourceCheckBox.isSelected
    fun isDatasourceCombinedSelected(): Boolean = combinedDatasourceCheckBox.isSelected
    fun isDatasourceRemoteSelected(): Boolean = remoteDatasourceCheckBox.isSelected
    fun isDatasourceLocalSelected(): Boolean = localDatasourceCheckBox.isSelected

    /** DI section getters */
    fun getIncludeDi(): Boolean = includeDiCheckBox.isSelected
    fun isDiHiltSelected(): Boolean = diHiltCheckBox.isSelected
    fun isDiKoinSelected(): Boolean = diKoinCheckBox.isSelected
}
