package com.github.jkjamies.cammp.feature.presentationgenerator.util

import junit.framework.TestCase

/**
 * Tests for [NameUtils].
 */
class NameUtilsTests : TestCase() {
    fun testLowerFirst() {
        assertEquals("hello", NameUtils.lowerFirst("Hello"))
        assertEquals("h", NameUtils.lowerFirst("h"))
        assertEquals("", NameUtils.lowerFirst(""))
    }
}
