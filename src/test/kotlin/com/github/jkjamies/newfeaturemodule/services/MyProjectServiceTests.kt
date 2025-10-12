package com.github.jkjamies.newfeaturemodule.services

import junit.framework.TestCase

/**
 * Smoke tests for [MyProjectService] construction.
 */
class MyProjectServiceTests : TestCase() {
    fun testServiceCanBeConstructed() {
        val service = MyProjectService()
        assertNotNull(service)
    }
}
