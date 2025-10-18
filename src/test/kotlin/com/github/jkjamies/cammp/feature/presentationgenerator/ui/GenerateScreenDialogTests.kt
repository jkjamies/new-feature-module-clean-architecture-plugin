package com.github.jkjamies.cammp.feature.presentationgenerator.ui

import junit.framework.TestCase

class GenerateScreenDialogTests : TestCase() {
    fun testDiChoiceEnum() {
        val values = GenerateScreenDialog.DiChoice.values()
        assertTrue(values.contains(GenerateScreenDialog.DiChoice.HILT))
        assertTrue(values.contains(GenerateScreenDialog.DiChoice.KOIN))
    }
}
