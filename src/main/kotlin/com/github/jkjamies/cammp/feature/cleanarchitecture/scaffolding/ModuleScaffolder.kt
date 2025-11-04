package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.github.jkjamies.cammp.feature.cleanarchitecture.util.FileUtilExt
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
    fun scaffoldModule(
        moduleDir: VirtualFile,
        moduleName: String,
        rootName: String,
        featureName: String,
        orgSegment: String
    ) = scaffoldModule(
        moduleDir,
        moduleName,
        rootName,
        featureName,
        orgSegment,
        includeDatasource = false,
        datasourceCombined = false,
        datasourceRemote = false,
        datasourceLocal = false
    )

    fun scaffoldModule(
        moduleDir: VirtualFile,
        moduleName: String,
        rootName: String,
        featureName: String,
        orgSegment: String,
        includeDatasource: Boolean = false,
        datasourceCombined: Boolean = false,
        datasourceRemote: Boolean = false,
        datasourceLocal: Boolean = false
    ) {
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
        // Use organized location under templates/cleanArchitecture/module only
        val candidatePaths = listOf(
            "templates/cleanArchitecture/module/${templateName}.gradle.kts"
        )
        val originalTemplate = run {
            var text: String? = null
            for (p in candidatePaths) {
                text = this::class.java.getResource("/$p")?.readText()
                    ?: this::class.java.classLoader.getResource(p)?.readText()
                if (text != null) break
            }
            text ?: DEFAULT_BUILD_GRADLE
        }

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

        // Additional clean architecture subdirectories
        when (moduleName) {
            "domain" -> {
                VfsUtil.createDirectories(pkgDir.path + "/repository").refresh(false, true)
                VfsUtil.createDirectories(pkgDir.path + "/model").refresh(false, true)
                VfsUtil.createDirectories(pkgDir.path + "/usecase").refresh(false, true)
            }
            "data" -> {
                VfsUtil.createDirectories(pkgDir.path + "/repository").refresh(false, true)
                // Datasource subdirectories inside data module package when requested via flags
                if (includeDatasource) {
                    if (datasourceCombined) {
                        VfsUtil.createDirectories(pkgDir.path + "/dataSource").refresh(false, true)
                    } else {
                        if (datasourceRemote) VfsUtil.createDirectories(pkgDir.path + "/remoteDataSource").refresh(false, true)
                        if (datasourceLocal) VfsUtil.createDirectories(pkgDir.path + "/localDataSource").refresh(false, true)
                    }
                }
            }
        }
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
