package com.github.jkjamies.cammp.feature.presentationgenerator.util

import java.util.Locale

object NameUtils {
    fun lowerFirst(input: String): String =
        if (input.isEmpty()) input else input.replaceFirstChar { it.lowercase(Locale.getDefault()) }
}
