package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui

import com.intellij.testFramework.LightPlatformTestCase

/**
 * Verifies the behavior of [GenerateModulesDialog], including default values and title.
 */
class GenerateModulesDialogTests : LightPlatformTestCase() {
    fun testGetValuesReturnsTrimmedFieldsAndDefaults() {
        val dialog = GenerateModulesDialog(project)

        val (defaultRoot, defaultFeature) = dialog.getValues()
        assertEquals("", defaultRoot)
        assertEquals("", defaultFeature)

        // access private UI fields via reflection for testing
        val rootField = dialog.javaClass.getDeclaredField("rootField").apply { isAccessible = true }
        // access private UI fields via reflection for testing
        val featureField = dialog.javaClass.getDeclaredField("featureField").apply { isAccessible = true }

        val rootTextField = rootField.get(dialog) as com.intellij.ui.components.JBTextField
        val featureTextField = featureField.get(dialog) as com.intellij.ui.components.JBTextField

        // leading/trailing spaces should be trimmed by getValues
        rootTextField.text = "  libs  "
        // leading/trailing spaces should be trimmed by getValues
        featureTextField.text = "  profiles  "

        val (root, feature) = dialog.getValues()
        assertEquals("libs", root)
        assertEquals("profiles", feature)
    }

    fun testDialogTitle() {
        val dialog = GenerateModulesDialog(project)
        assertEquals("Generate Clean Architecture Modules", dialog.title)
    }

    fun testDatasourceSectionBehavior() {
        val dialog = GenerateModulesDialog(project)

        // includeDatasource should be false by default and options panel should be visible but disabled
        val includeDatasourceMethod = dialog.javaClass.getMethod("getIncludeDatasource")
        assertFalse(includeDatasourceMethod.invoke(dialog) as Boolean)

        // Access private panel to check visibility via reflection
        val optionsPanelField = dialog.javaClass.getDeclaredField("datasourceOptionsPanel").apply { isAccessible = true }
        val optionsPanel = optionsPanelField.get(dialog) as java.awt.Component
        assertTrue(optionsPanel.isVisible)

        // Access datasource option checkboxes
        val combinedField = dialog.javaClass.getDeclaredField("combinedDatasourceCheckBox").apply { isAccessible = true }
        val remoteField = dialog.javaClass.getDeclaredField("remoteDatasourceCheckBox").apply { isAccessible = true }
        val localField = dialog.javaClass.getDeclaredField("localDatasourceCheckBox").apply { isAccessible = true }
        val combined = combinedField.get(dialog) as com.intellij.ui.components.JBCheckBox
        val remote = remoteField.get(dialog) as com.intellij.ui.components.JBCheckBox
        val local = localField.get(dialog) as com.intellij.ui.components.JBCheckBox

        // When include is off, all options are disabled and unselected
        assertFalse(combined.isSelected)
        assertFalse(remote.isSelected)
        assertFalse(local.isSelected)
        assertFalse(combined.isEnabled)
        assertFalse(remote.isEnabled)
        assertFalse(local.isEnabled)

        // Now enable include datasource
        val includeDatasourceField = dialog.javaClass.getDeclaredField("includeDatasourceCheckBox").apply { isAccessible = true }
        val includeDatasourceCheckbox = includeDatasourceField.get(dialog) as com.intellij.ui.components.JBCheckBox
        includeDatasourceCheckbox.isSelected = true

        // By default when enabled: remote and local are selected, combined is not and all are enabled
        assertFalse(combined.isSelected)
        assertTrue(remote.isSelected)
        assertTrue(local.isSelected)
        assertTrue(remote.isEnabled)
        assertTrue(local.isEnabled)
        assertTrue(combined.isEnabled)

        // Selecting combined should clear remote/local and disable them
        combined.isSelected = true
        assertTrue(combined.isSelected)
        assertFalse(remote.isSelected)
        assertFalse(local.isSelected)
        assertFalse(remote.isEnabled)
        assertFalse(local.isEnabled)

        // Selecting remote should unselect combined
        remote.isEnabled = true
        local.isEnabled = true
        remote.isSelected = true
        assertFalse(combined.isSelected)
        // With only remote selected, remote becomes disabled and local remains enabled
        assertTrue(remote.isSelected)
        assertFalse(local.isSelected)
        assertFalse(remote.isEnabled)
        assertTrue(local.isEnabled)

        // Select local as well - both can be selected together; both should be enabled
        local.isSelected = true
        assertTrue(remote.isSelected && local.isSelected)
        assertTrue(remote.isEnabled)
        assertTrue(local.isEnabled)

        // Turning off include datasource clears all selections and disables them; panel remains visible
        includeDatasourceCheckbox.isSelected = false
        assertFalse(combined.isSelected)
        assertFalse(remote.isSelected)
        assertFalse(local.isSelected)
        assertFalse(combined.isEnabled)
        assertFalse(remote.isEnabled)
        assertFalse(local.isEnabled)
        assertTrue(optionsPanel.isVisible)
    }
    fun testDiSectionBehavior() {
        val dialog = GenerateModulesDialog(project)

        // Include DI should be true by default and options panel should be visible
        val includeDiMethod = dialog.javaClass.getMethod("getIncludeDi")
        assertTrue(includeDiMethod.invoke(dialog) as Boolean)

        // Access DI options panel and radio buttons via reflection
        val diPanelField = dialog.javaClass.getDeclaredField("diOptionsPanel").apply { isAccessible = true }
        val diPanel = diPanelField.get(dialog) as java.awt.Component
        assertTrue(diPanel.isVisible)

        val hiltField = dialog.javaClass.getDeclaredField("diHiltRadioButton").apply { isAccessible = true }
        val koinField = dialog.javaClass.getDeclaredField("diKoinRadioButton").apply { isAccessible = true }
        val koinAnnotationsField = dialog.javaClass.getDeclaredField("koinAnnotationsCheckBox").apply { isAccessible = true }
        val hilt = hiltField.get(dialog) as com.intellij.ui.components.JBRadioButton
        val koin = koinField.get(dialog) as com.intellij.ui.components.JBRadioButton
        val koinAnnotations = koinAnnotationsField.get(dialog) as com.intellij.ui.components.JBCheckBox

        // Defaults when enabled: Hilt selected, Koin not; with radio buttons both remain enabled
        assertTrue(hilt.isSelected)
        assertFalse(koin.isSelected)
        assertTrue(hilt.isEnabled)
        assertTrue(koin.isEnabled)
        // Koin annotations should be hidden and disabled by default (since Hilt is selected)
        assertFalse(koinAnnotations.isVisible)
        assertFalse(koinAnnotations.isEnabled)
        assertFalse(koinAnnotations.isSelected)

        // Selecting Koin should unselect Hilt; both remain enabled; Koin annotations should appear and be enabled
        koin.isSelected = true
        assertTrue(koin.isSelected)
        assertFalse(hilt.isSelected)
        assertTrue(koin.isEnabled)
        assertTrue(hilt.isEnabled)
        assertTrue(koinAnnotations.isVisible)
        assertTrue(koinAnnotations.isEnabled)

        // Disable Include DI: both options become unselected and disabled, and Koin annotations hidden and cleared
        val includeDiField = dialog.javaClass.getDeclaredField("includeDiCheckBox").apply { isAccessible = true }
        val includeDiCheckbox = includeDiField.get(dialog) as com.intellij.ui.components.JBCheckBox
        includeDiCheckbox.isSelected = false
        assertFalse(hilt.isSelected)
        assertFalse(koin.isSelected)
        assertFalse(hilt.isEnabled)
        assertFalse(koin.isEnabled)
        assertFalse(koinAnnotations.isVisible)
        assertFalse(koinAnnotations.isEnabled)
        assertFalse(koinAnnotations.isSelected)

        // Re-enable Include DI: default should select Hilt again and both radios stay enabled; Koin annotations remains hidden
        includeDiCheckbox.isSelected = true
        assertTrue(hilt.isSelected)
        assertFalse(koin.isSelected)
        assertTrue(hilt.isEnabled)
        assertTrue(koin.isEnabled)
        assertFalse(koinAnnotations.isVisible)
        assertFalse(koinAnnotations.isEnabled)
        assertFalse(koinAnnotations.isSelected)

        // Switch to Koin again -> annotations visible and enabled
        koin.isSelected = true
        assertTrue(koinAnnotations.isVisible)
        assertTrue(koinAnnotations.isEnabled)
    }
    fun testPlatformSectionAndKmpDiConstraint() {
        val dialog = GenerateModulesDialog(project)

        // Access platform radios
        val androidField = dialog.javaClass.getDeclaredField("platformAndroidRadioButton").apply { isAccessible = true }
        val kmpField = dialog.javaClass.getDeclaredField("platformKmpRadioButton").apply { isAccessible = true }
        val android = androidField.get(dialog) as com.intellij.ui.components.JBRadioButton
        val kmp = kmpField.get(dialog) as com.intellij.ui.components.JBRadioButton

        // Access DI radios
        val hiltField = dialog.javaClass.getDeclaredField("diHiltRadioButton").apply { isAccessible = true }
        val koinField = dialog.javaClass.getDeclaredField("diKoinRadioButton").apply { isAccessible = true }
        val hilt = hiltField.get(dialog) as com.intellij.ui.components.JBRadioButton
        val koin = koinField.get(dialog) as com.intellij.ui.components.JBRadioButton

        // Defaults: Android selected, DI enabled, Hilt selected and both enabled
        assertTrue(android.isSelected)
        assertFalse(kmp.isSelected)
        assertTrue(hilt.isSelected)
        assertFalse(koin.isSelected)
        assertTrue(hilt.isEnabled)
        assertTrue(koin.isEnabled)

        // Select KMP -> Hilt must be disabled and unselected; Koin must be selected and enabled
        kmp.isSelected = true
        assertTrue(kmp.isSelected)
        assertFalse(android.isSelected)
        assertFalse(hilt.isSelected)
        assertTrue(koin.isSelected)
        assertFalse(hilt.isEnabled)
        assertTrue(koin.isEnabled)

        // Switch back to Android -> Hilt becomes enabled again (selection may remain Koin)
        android.isSelected = true
        assertTrue(android.isSelected)
        assertFalse(kmp.isSelected)
        assertTrue(hilt.isEnabled)
        assertTrue(koin.isEnabled)
    }
}
