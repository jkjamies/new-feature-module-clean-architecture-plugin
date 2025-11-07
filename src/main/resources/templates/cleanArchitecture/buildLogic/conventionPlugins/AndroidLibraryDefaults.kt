package com.${PACKAGE}.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared Android library defaults for Android library convention plugins.
 */
internal fun Project.configureAndroidLibraryDefaults() {
    extensions.configure<LibraryExtension> {
        compileSdk = 36
        defaultConfig {
            minSdk = 27
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    // Align Kotlin JVM target with Java to avoid mismatch
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(17)
    }
}
