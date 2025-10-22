package com.github.jkjamies.cammp.feature.repositorygenerator.ui

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.testFramework.LightPlatformTestCase
import javax.swing.JTextField

/**
 * Tests for [GenerateRepositoryDialog].
 */
class GenerateRepositoryDialogTests : LightPlatformTestCase() {
    fun testDefaultDiSelections() {
        val dialog = GenerateRepositoryDialog(project)
        // Do not show the dialog; just verify default states
        assertTrue(dialog.isDiEnabled())
        assertTrue(dialog.isHiltSelected())
        assertFalse(dialog.isKoinAnnotationsSelected())
    }

    fun testDirFieldIsBrowseControl() {
        val dialog = GenerateRepositoryDialog(project)
        val f = dialog.javaClass.getDeclaredField("dirField").apply { isAccessible = true }
        val value = f.get(dialog)
        assertTrue(value is TextFieldWithBrowseButton)
    }

    fun testValidationRequiresDataModuleAndName() {
        val dialog = GenerateRepositoryDialog(project)
        val dirField = dialog.javaClass.getDeclaredField("dirField").apply { isAccessible = true }
        val tfwb = dirField.get(dialog) as TextFieldWithBrowseButton
        val nameField = dialog.javaClass.getDeclaredField("nameField").apply { isAccessible = true }
        val nameTf = nameField.get(dialog) as JTextField

        tfwb.text = "/tmp/not-a-data" // not ending with 'data'
        nameTf.text = ""

        val m = dialog.javaClass.getDeclaredMethod("doValidateAll").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val errors = m.invoke(dialog) as List<ValidationInfo>
        assertTrue(errors.any { it.message.contains("data module", ignoreCase = true) })
        assertTrue(errors.any { it.message.contains("repository name", ignoreCase = true) })
    }
}
