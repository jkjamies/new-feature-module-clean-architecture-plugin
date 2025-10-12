package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.ui

import com.intellij.testFramework.LightPlatformTestCase

/**
 * Verifies the behavior of [GenerateModulesDialog], including default values and title.
 */
class GenerateModulesDialogTests : LightPlatformTestCase() {
    fun testGetValuesReturnsTrimmedFieldsAndDefaults() {
        val dialog = GenerateModulesDialog(project)

        val (defaultRoot, defaultFeature) = dialog.getValues()
        assertEquals("features", defaultRoot)
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
}
