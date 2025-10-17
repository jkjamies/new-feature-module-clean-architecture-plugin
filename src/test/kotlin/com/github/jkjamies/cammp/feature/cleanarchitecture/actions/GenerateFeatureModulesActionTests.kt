package com.github.jkjamies.cammp.feature.cleanarchitecture.actions

import com.intellij.openapi.actionSystem.AnAction
import junit.framework.TestCase

/**
 * Tests that [GenerateFeatureModulesAction] advertises the expected presentation text.
 */
class GenerateFeatureModulesActionTests : TestCase() {
    fun testActionHasCorrectPresentationText() {
        val action: AnAction = GenerateFeatureModulesAction()
        assertEquals("Generate Clean Architecture Modules", action.templatePresentation.text)
    }
}
