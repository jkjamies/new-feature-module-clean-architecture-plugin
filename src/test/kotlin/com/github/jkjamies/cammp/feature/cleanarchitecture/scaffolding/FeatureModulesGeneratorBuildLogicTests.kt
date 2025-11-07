package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

class FeatureModulesGeneratorBuildLogicTests : LightPlatformTestCase() {
    fun testBuildLogicCreatedWithRelevantPlugins() {
        val tempProject = Files.createTempDirectory("fm-gen-build-logic").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "payments",
                includePresentation = true,
                includeDatasource = false,
                datasourceCombined = false,
                datasourceRemote = false,
                datasourceLocal = false,
                includeDi = true,
                orgSegment = "mcdonalds"
            )
        }

        val buildLogicDir = projectRootVf.findChild("build-logic") ?: error("build-logic not created")
        // check build.gradle.kts exists
        val buildGradle = buildLogicDir.findChild("build.gradle.kts") ?: error("build.gradle.kts missing")
        val buildText = VfsUtil.loadText(buildGradle)
        // should contain registrations for data, di, domain, presentation
        assertTrue(buildText.contains("com.mcdonalds.convention.DataConventionPlugin") || buildText.contains("DataConventionPlugin"))
        assertTrue(buildText.contains("com.mcdonalds.convention.DiConventionPlugin") || buildText.contains("DiConventionPlugin"))
        assertTrue(buildText.contains("com.mcdonalds.convention.DomainConventionPlugin") || buildText.contains("DomainConventionPlugin"))
        assertTrue(buildText.contains("com.mcdonalds.convention.PresentationConventionPlugin") || buildText.contains("PresentationConventionPlugin"))

        // check convention Kotlin files exist and have correct package line
        val srcMain = VfsUtil.findRelativeFile("build-logic/src/main/kotlin/com/mcdonalds/convention", projectRootVf)
            ?: error("convention src dir missing")
        listOf("DataConventionPlugin.kt", "DIConventionPlugin.kt", "DomainConventionPlugin.kt", "PresentationConventionPlugin.kt").forEach { f ->
            val vf = srcMain.findChild(f) ?: error("$f missing in build-logic")
            val text = VfsUtil.loadText(vf)
            assertTrue(text.contains("package com.mcdonalds.convention"))
        }
    }

    fun testDatasourcePluginsOnlyWhenEnabled() {
        val tempProject = Files.createTempDirectory("fm-gen-build-logic-ds").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "catalog",
                includePresentation = true,
                includeDatasource = true,
                datasourceCombined = true,
                datasourceRemote = false,
                datasourceLocal = false,
                includeDi = true,
                orgSegment = "acme"
            )
        }

        val srcMain = VfsUtil.findRelativeFile("build-logic/src/main/kotlin/com/acme/convention", projectRootVf)
            ?: error("convention src dir missing")
        // When combined datasource selected, DataSourceConventionPlugin should exist, but remote/local shouldn't
        assertNotNull(srcMain.findChild("DataSourceConventionPlugin.kt"))
        assertNull(srcMain.findChild("RemoteDataSourceConventionPlugin.kt"))
        assertNull(srcMain.findChild("LocalDataSourceConventionPlugin.kt"))
    }
}

