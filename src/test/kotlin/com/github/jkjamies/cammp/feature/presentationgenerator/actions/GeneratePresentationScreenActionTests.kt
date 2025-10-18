package com.github.jkjamies.cammp.feature.presentationgenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import junit.framework.TestCase

class GeneratePresentationScreenActionTests : TestCase() {
    fun testActionHasCorrectPresentationText() {
        val action: AnAction = GeneratePresentationScreenAction()
        assertEquals("Generate Presentation Screen", action.templatePresentation.text)
    }
}
