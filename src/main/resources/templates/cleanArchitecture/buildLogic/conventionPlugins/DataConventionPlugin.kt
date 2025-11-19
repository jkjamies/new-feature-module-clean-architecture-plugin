package com.${PACKAGE}.convention

import com.${PACKAGE}.convention.helpers.configureAndroidLibraryDefaults
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class DataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Use plugin coordinates from the version catalog (libs)
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val androidLibraryPluginId = libs.findPlugin("library").get().get().pluginId
        val kotlinAndroidPluginId = libs.findPlugin("kotlin").get().get().pluginId
        val kspPluginId = libs.findPlugin("ksp").get().get().pluginId
        val hiltPluginId = libs.findPlugin("hilt").get().get().pluginId
        val parcelizePluginId = libs.findPlugin("parcelize").get().get().pluginId
        val kotlinSerializationPluginId = libs.findPlugin("kotlin-serialization").get().get().pluginId

        // Apply plugins
        pluginManager.apply(androidLibraryPluginId)
        pluginManager.apply(kotlinAndroidPluginId)
        pluginManager.apply(kspPluginId)
        pluginManager.apply(hiltPluginId)
        pluginManager.apply(parcelizePluginId)
        pluginManager.apply(kotlinSerializationPluginId)

        // TODO: apply other plugins or code coverage (jacoco), etc

        // Configure Android library defaults
        configureAndroidLibraryDefaults()

        // Use alias coordinates from the version catalog (libs)
        val hiltAndroid = libs.findLibrary("hilt").get()
        val hiltCompiler = libs.findLibrary("hilt-compiler").get()
        val kotlinxSerialization = libs.findLibrary("kotlinx-serialization").get()

        dependencies {
            addProvider("implementation", hiltAndroid)
            addProvider("implementation", kotlinxSerialization)
            addProvider("ksp", hiltCompiler)
        }
    }
}
