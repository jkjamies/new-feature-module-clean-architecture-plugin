package com.${PACKAGE}.convention

import com.${PACKAGE}.convention.helpers.configureAndroidLibraryDefaults
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure

class PresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // Use plugin coordinates from the version catalog (libs)
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val androidLibraryPluginId = libs.findPlugin("library").get().get().pluginId
        val kotlinAndroidPluginId = libs.findPlugin("kotlin").get().get().pluginId
        val kspPluginId = libs.findPlugin("ksp").get().get().pluginId
        val hiltPluginId = libs.findPlugin("hilt").get().get().pluginId
        val parcelizePluginId = libs.findPlugin("parcelize").get().get().pluginId
        val kotlinSerializationPluginId = libs.findPlugin("kotlin-serialization").get().get().pluginId
        val composeCompilerPluginId = libs.findPlugin("compose-compiler").get().get().pluginId

        // Apply plugins
        pluginManager.apply(androidLibraryPluginId)
        pluginManager.apply(kotlinAndroidPluginId)
        pluginManager.apply(kspPluginId)
        pluginManager.apply(hiltPluginId)
        pluginManager.apply(parcelizePluginId)
        pluginManager.apply(kotlinSerializationPluginId)
        pluginManager.apply(composeCompilerPluginId)

        // TODO: apply other plugins or code coverage (jacoco), etc

        // Configure Android library defaults
        configureAndroidLibraryDefaults()

        // Additional Android library configuration for presentation layer
        extensions.configure<LibraryExtension> {
            buildFeatures {
                compose = true
            }

            packaging {
                resources {
                    excludes += "META-INF/*"
                    excludes += "draftv4/schema"
                    excludes += "draftv3/schema"
                    excludes += "google/protobuf/*"
                }
            }
        }

        // Use alias coordinates from the version catalog (libs)
        val hiltAndroid = libs.findLibrary("hilt").get()
        val hiltCompiler = libs.findLibrary("hilt-compiler").get()
        val kotlinxSerialization = libs.findLibrary("kotlinx-serialization").get()
        val coroutinesAndroid = libs.findLibrary("coroutines-android").get()
        val coroutinesCore = libs.findLibrary("coroutines-core").get()
        val composeMaterial3Android = libs.findLibrary("compose-material3-android").get()
        val coreKtx = libs.findLibrary("androidx-core-ktx").get()
        val composeUi = libs.findLibrary("compose-ui").get()
        val composeNavigation = libs.findLibrary("compose-navigation").get()
        val composeHiltNavigation = libs.findLibrary("compose-hilt-navigation").get()
        val composeTooling = libs.findLibrary("compose-tooling").get()
        val composePreview = libs.findLibrary("compose-preview").get()

        // Instrumented test dependencies
        val testRunner = libs.findLibrary("androidx-test-runner").get()
        val composeUiTest = libs.findLibrary("compose-ui-test").get()
        val mockkAndroid = libs.findLibrary("mockk-android").get()
        val coroutinesTest = libs.findLibrary("coroutines-test").get()
        val espresso = libs.findLibrary("espresso").get()

        dependencies {
            // Implementation dependencies via addProvider
            addProvider("implementation", hiltAndroid)
            addProvider("implementation", kotlinxSerialization)
            addProvider("implementation", coroutinesAndroid)
            addProvider("implementation", coroutinesCore)
            addProvider("implementation", composeMaterial3Android)
            addProvider("implementation", coreKtx)
            // Compose dependencies
            addProvider("implementation", composeUi)
            addProvider("implementation", composeNavigation)
            addProvider("implementation", composeHiltNavigation)
            addProvider("implementation", composeTooling)
            addProvider("implementation", composePreview)

            // KSP compiler dependency
            addProvider("ksp", hiltCompiler)

            // Instrumented test dependencies
            add("androidTestImplementation", testRunner)
            add("androidTestImplementation", composeUiTest)
            add("androidTestImplementation", mockkAndroid)
            add("androidTestImplementation", coroutinesTest)
            add("androidTestImplementation", espresso)
        }
    }
}
