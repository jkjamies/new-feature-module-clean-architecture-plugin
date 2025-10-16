package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util.FileUtilExt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

/**
 * Creates the basic Gradle and source structure for a single feature submodule.
 */
class ModuleScaffolder {
    /**
     * Ensures Gradle build file and source directories exist under [moduleDir] and writes
     * a simple placeholder Kotlin file in package `com.<orgSegment>.[rootName].[featureName].[moduleName]`.
     */
    fun scaffoldModule(moduleDir: VirtualFile, moduleName: String, rootName: String, featureName: String, orgSegment: String) {
        val templateName = when (moduleName) {
            "domain" -> "domain"
            "data" -> "data"
            "di" -> "di"
            "presentation" -> "presentation"
            "dataSource" -> "dataSource"
            "remoteDataSource" -> "remoteDataSource"
            "localDataSource" -> "localDataSource"
            else -> "domain"
        }
        val resourcePath = "templates/${templateName}.gradle.kts"
        val originalTemplate = this::class.java.getResource("/$resourcePath")?.readText()
            ?: this::class.java.classLoader.getResource(resourcePath)?.readText()
            ?: DEFAULT_BUILD_GRADLE

        val safeOrg = orgSegment.trim().ifEmpty { "jkjamies" }
        val packageName = "com.$safeOrg.$rootName.$featureName.$moduleName"

        // Adapt template placeholders to match the computed package for all modules
        val buildText = originalTemplate
            .replace("NAMESPACE", packageName)
            .replace("com.jkjamies.imgur.api", packageName)

        // avoid overwriting if user customized later
        FileUtilExt.writeFileIfAbsent(moduleDir, "build.gradle.kts", buildText)

        val srcMainKotlin = VfsUtil.createDirectories(moduleDir.path + "/src/main/kotlin")
        val srcMainResources = VfsUtil.createDirectories(moduleDir.path + "/src/main/resources")
        val srcTestKotlin = VfsUtil.createDirectories(moduleDir.path + "/src/test/kotlin")
        srcMainKotlin.refresh(false, true)
        srcMainResources.refresh(false, true)
        srcTestKotlin.refresh(false, true)

        val placeholder = """
            package $packageName
            
            class Placeholder
        """.trimIndent()
        // convert dotted package to folder path
        val pkgDirPath = srcMainKotlin.path + "/" + packageName.replace('.', '/')
        val pkgDir = VfsUtil.createDirectories(pkgDirPath)
        // create only if missing
        FileUtilExt.writeFileIfAbsent(pkgDir, "Placeholder.kt", placeholder)
    }

    companion object {
        private val DEFAULT_BUILD_GRADLE = """
            plugins {
                `java-library`
            }
            
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }
            
            group = "com.example"
            version = "1.0.0"
            
            repositories {
                mavenCentral()
                google()
            }
            
            dependencies {
            }
        """.trimIndent()
    }
}
