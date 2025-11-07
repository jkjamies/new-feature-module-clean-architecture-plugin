package com.${PACKAGE}.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class PresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Use plugin coordinates from the version catalog (libs)
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val androidLibraryPluginId = libs.findPlugin("android-library").get().get().pluginId
        val kotlinAndroidPluginId = libs.findPlugin("kotlin-android").get().get().pluginId
        val kspPluginId = libs.findPlugin("ksp").get().get().pluginId
        val hiltPluginId = libs.findPlugin("hilt").get().get().pluginId

        pluginManager.apply(androidLibraryPluginId)
        pluginManager.apply(kotlinAndroidPluginId)
        pluginManager.apply(kspPluginId)
        pluginManager.apply(hiltPluginId)

        configureAndroidLibraryDefaults()

        val hiltAndroid = libs.findLibrary("hilt-android").get()
        val hiltCompiler = libs.findLibrary("hilt-compiler").get()

        dependencies {
            addProvider("implementation", hiltAndroid)
            addProvider("ksp", hiltCompiler)
        }
    }
}
