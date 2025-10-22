package com.github.jkjamies.cammp.feature.usecasegenerator.ui

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.testFramework.LightPlatformTestCase
import javax.swing.JComponent
import javax.swing.JTextField

class GenerateUseCaseDialogTests : LightPlatformTestCase() {
    fun testDefaultDiSelections() {
        val dialog = GenerateUseCaseDialog(project)
        assertTrue(dialog.isDiEnabled())
        assertTrue(dialog.isHiltSelected())
        assertFalse(dialog.isKoinAnnotationsSelected())
    }

    fun testDirFieldIsBrowseControl() {
        val dialog = GenerateUseCaseDialog(project)
        val f = dialog.javaClass.getDeclaredField("dirField").apply { isAccessible = true }
        val value = f.get(dialog)
        assertTrue(value is TextFieldWithBrowseButton)
    }

    fun testCreateCenterPanelIsWideEnough() {
        val dialog = GenerateUseCaseDialog(project)
        val m = dialog.javaClass.getDeclaredMethod("createCenterPanel").apply { isAccessible = true }
        val center = m.invoke(dialog) as JComponent
        assertTrue("Preferred width should be at least 1100", center.preferredSize.width >= 1100)
        assertTrue("Minimum width should be at least 900", center.minimumSize.width >= 900)
    }

    fun testGetUseCaseNameAppendsSuffix() {
        val dialog = GenerateUseCaseDialog(project)
        val nameField = dialog.javaClass.getDeclaredField("nameField").apply { isAccessible = true }
        val tf = nameField.get(dialog) as JTextField
        tf.text = "FetchUser"
        assertEquals("FetchUserUseCase", dialog.getUseCaseName())
        tf.text = "DoWorkUseCase"
        assertEquals("DoWorkUseCase", dialog.getUseCaseName())
    }

    fun testValidationRequiresDomainModule() {
        val dialog = GenerateUseCaseDialog(project)
        val dirField = dialog.javaClass.getDeclaredField("dirField").apply { isAccessible = true }
        val tf = (dirField.get(dialog) as TextFieldWithBrowseButton).textField
        val nameField = dialog.javaClass.getDeclaredField("nameField").apply { isAccessible = true }
        val nameTf = nameField.get(dialog) as JTextField

        tf.text = "/tmp/not-a-domain" // not ending with 'domain'
        nameTf.text = "Test"

        val m = dialog.javaClass.getDeclaredMethod("doValidateAll").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val errors = m.invoke(dialog) as List<ValidationInfo>
        assertTrue(errors.any { it.message.contains("domain", ignoreCase = true) })
    }
}
