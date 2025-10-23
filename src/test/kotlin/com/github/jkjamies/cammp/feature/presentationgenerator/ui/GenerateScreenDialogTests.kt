package com.github.jkjamies.cammp.feature.presentationgenerator.ui

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.components.JBCheckBox

/**
 * Tests for [GenerateScreenDialog].
 */
class GenerateScreenDialogTests : LightPlatformTestCase() {
    fun testDiChoiceEnum() {
        val values = GenerateScreenDialog.DiChoice.entries.toTypedArray()
        assertTrue(values.contains(GenerateScreenDialog.DiChoice.HILT))
        assertTrue(values.contains(GenerateScreenDialog.DiChoice.KOIN))
    }

    fun testPatternChoiceEnum() {
        val values = GenerateScreenDialog.PatternChoice.entries.toTypedArray()
        assertTrue(values.contains(GenerateScreenDialog.PatternChoice.MVI))
        assertTrue(values.contains(GenerateScreenDialog.PatternChoice.MVVM))
    }

    fun testUseCaseSelection_emptyByDefault() {
        val dialog = GenerateScreenDialog(project)
        // By default, nothing selected and list may be empty if scan fails in tests.
        assertTrue(dialog.getSelectedUseCaseFqns().isEmpty())
        assertTrue(dialog.getSelectedUseCaseGradlePaths().isEmpty())
    }

    fun testUseCaseSelection_returnsSelectedFqnsAndGradlePaths() {
        val dialog = GenerateScreenDialog(project)

        // Reflect into private data list: MutableList<Pair<JBCheckBox, UseCaseItem>>
        val listField = dialog.javaClass.getDeclaredField("useCaseCheckboxes").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val pairs = listField.get(dialog) as MutableList<Any>

        // Private nested class UseCaseItem(moduleGradlePath: String, fqn: String)
        val ucItemClass = dialog.javaClass.declaredClasses.first { it.simpleName == "UseCaseItem" }
        val ctor = ucItemClass.getDeclaredConstructor(String::class.java, String::class.java).apply { isAccessible = true }

        fun addItem(gradlePath: String, fqn: String, selected: Boolean) {
            val cb = JBCheckBox(fqn, selected)
            val uc = ctor.newInstance(gradlePath, fqn)
            // Kotlin Pair at runtime
            val pair = Pair(cb, uc)
            pairs.add(pair)
        }

        // Seed three usecases from two modules; select two of them
        addItem(":features:payments:domain", "com.acme.payments.GetPaymentsUseCase", selected = true)
        addItem(":features:payments:domain", "com.acme.payments.RefreshPaymentsUseCase", selected = false)
        addItem(":features:users:domain", "com.acme.users.GetUserUseCase", selected = true)

        val fqns = dialog.getSelectedUseCaseFqns()
        val paths = dialog.getSelectedUseCaseGradlePaths()

        // FQNs should include exactly the selected ones (order preserved by insertion)
        assertEquals(listOf(
            "com.acme.payments.GetPaymentsUseCase",
            "com.acme.users.GetUserUseCase"
        ), fqns)

        // Gradle paths should be a distinct set of modules for selected items
        assertEquals(setOf(
            ":features:payments:domain",
            ":features:users:domain"
        ), paths)

        // Toggle selection and verify updates propagate
        // Turn off the first, enable the middle one
        val firstCb = (pairs[0] as Pair<*, *>).first as JBCheckBox
        val secondCb = (pairs[1] as Pair<*, *>).first as JBCheckBox
        firstCb.isSelected = false
        secondCb.isSelected = true

        val fqns2 = dialog.getSelectedUseCaseFqns()
        assertEquals(listOf(
            "com.acme.payments.RefreshPaymentsUseCase",
            "com.acme.users.GetUserUseCase"
        ), fqns2)
    }
}
