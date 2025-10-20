package com.github.jkjamies.cammp.feature.repositorygenerator.ui

import com.intellij.testFramework.LightPlatformTestCase

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
}
