package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util

object GradlePathUtil {
    fun gradlePathFor(projectBasePath: String, featurePath: String, moduleName: String): String {
        val rel = featurePath.removePrefix(projectBasePath).trimStart('/', '\\')
        val segments = (if (rel.isEmpty()) listOf() else rel.split('/', '\\')) + moduleName
        return ":" + segments.joinToString(":")
    }
}
