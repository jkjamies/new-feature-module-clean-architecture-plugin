package com.github.jkjamies.cammp.feature.presentationgenerator.ui

import junit.framework.TestCase

/**
 * Tests for [GenerateScreenDialog].
 */
class GenerateScreenDialogTests : TestCase() {
    fun testDiChoiceEnum() {
        val values = GenerateScreenDialog.DiChoice.values()
        assertTrue(values.contains(GenerateScreenDialog.DiChoice.HILT))
        assertTrue(values.contains(GenerateScreenDialog.DiChoice.KOIN))
    }

    fun testPatternChoiceEnum() {
        val values = GenerateScreenDialog.PatternChoice.values()
        assertTrue(values.contains(GenerateScreenDialog.PatternChoice.MVI))
        assertTrue(values.contains(GenerateScreenDialog.PatternChoice.MVVM))
    }
}
