package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util

import junit.framework.TestCase

/**
 * Tests for [GradlePathUtil] handling diverse path separator scenarios.
 */
class GradlePathUtilTests : TestCase() {

    fun testPathWhenFeatureAtProjectRoot() {
        val base = "/Users/me/project"
        val feature = "/Users/me/project"
        val actual = GradlePathUtil.gradlePathFor(base, feature, "domain")
        assertEquals(":domain", actual)
    }

    fun testPathWithNestedUnixDirs() {
        val base = "/work/repo"
        val feature = "/work/repo/features/payments"
        val actual = GradlePathUtil.gradlePathFor(base, feature, "data")
        assertEquals(":features:payments:data", actual)
    }

    fun testPathWithWindowsSeparators() {
        val base = "C\\projects\\repo"
        val feature = "C\\projects\\repo\\features\\profile"
        val actual = GradlePathUtil.gradlePathFor(base, feature, "presentation")
        assertEquals(":features:profile:presentation", actual)
    }

    fun testPathWithExtraSeparators() {
        val base = "/a/b"
        val feature = "/a/b//features///auth/"
        val actual = GradlePathUtil.gradlePathFor(base, feature, "di")
        assertEquals(":features:auth:di", actual)
    }
}
