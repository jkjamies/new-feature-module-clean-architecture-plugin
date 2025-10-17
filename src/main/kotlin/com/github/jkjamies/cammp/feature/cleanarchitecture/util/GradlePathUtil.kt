package com.github.jkjamies.cammp.feature.cleanarchitecture.util

/**
 * Utilities for constructing Gradle include paths from filesystem paths.
 *
 * Converts platform-specific separators to forward slashes and produces a colon-separated
 * include string such as `:features:payments:domain`.
 */
object GradlePathUtil {
    /**
     * Builds a Gradle include path for [moduleName] given the absolute [projectBasePath]
     * and the absolute [featurePath] which points to the feature directory.
     */
    fun gradlePathFor(projectBasePath: String, featurePath: String, moduleName: String): String {
        // normalize Windows backslashes to Unix style
        val normBase = projectBasePath.replace('\\', '/')
        // normalize Windows backslashes to Unix style
        val normFeature = featurePath.replace('\\', '/')
        // derive feature path relative to project root if possible
        var rel = if (normFeature.startsWith(normBase)) normFeature.removePrefix(normBase) else normFeature
        // drop any leading slash to avoid empty first segment
        rel = rel.trimStart('/')
        // collapse duplicate separators and append module name
        val segments = rel.split('/').filter { it.isNotEmpty() } + moduleName
        // Gradle uses colon-separated module paths
        return ":" + segments.joinToString(":")
    }
}
