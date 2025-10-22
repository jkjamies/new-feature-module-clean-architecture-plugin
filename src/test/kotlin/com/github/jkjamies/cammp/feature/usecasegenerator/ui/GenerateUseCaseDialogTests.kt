package com.github.jkjamies.cammp.feature.usecasegenerator.ui

import com.intellij.testFramework.LightPlatformTestCase

class GenerateUseCaseDialogTests : LightPlatformTestCase() {
    fun testDefaultDiSelections() {
        val dialog = GenerateUseCaseDialog(project)
        assertTrue(dialog.isDiEnabled())
        assertTrue(dialog.isHiltSelected())
        assertFalse(dialog.isKoinAnnotationsSelected())
    }
}

