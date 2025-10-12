package com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.newfeaturemodule.feature.cleanarchitecture.util.FileUtilExt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

/** Creates per-module folder structure and minimal files. */
class ModuleScaffolder {
    fun scaffoldModule(moduleDir: VirtualFile, moduleName: String, rootName: String, featureName: String) {
        // build.gradle.kts
        val buildText = """
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
            
            // Add your module-specific dependencies here
            dependencies {
            }
        """.trimIndent()
        FileUtilExt.writeFileIfAbsent(moduleDir, "build.gradle.kts", buildText)

        // Kotlin source sets
        val srcMainKotlin = VfsUtil.createDirectories(moduleDir.path + "/src/main/kotlin")
        val srcMainResources = VfsUtil.createDirectories(moduleDir.path + "/src/main/resources")
        val srcTestKotlin = VfsUtil.createDirectories(moduleDir.path + "/src/test/kotlin")
        srcMainKotlin.refresh(false, true)
        srcMainResources.refresh(false, true)
        srcTestKotlin.refresh(false, true)

        val packageName = "com.jkjamies.$rootName.$featureName.$moduleName"
        val placeholder = """
            package $packageName
            
            class Placeholder
        """.trimIndent()
        val pkgDirPath = srcMainKotlin.path + "/" + packageName.replace('.', '/')
        val pkgDir = VfsUtil.createDirectories(pkgDirPath)
        FileUtilExt.writeFileIfAbsent(pkgDir, "Placeholder.kt", placeholder)
    }
}
