package com.github.jkjamies.cammp.feature.usecasegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import junit.framework.TestCase

class GenerateUseCaseActionTests : TestCase() {
    fun testActionHasCorrectPresentationText() {
        val action: AnAction = GenerateUseCaseAction()
        assertEquals("Generate UseCase", action.templatePresentation.text)
    }
}

