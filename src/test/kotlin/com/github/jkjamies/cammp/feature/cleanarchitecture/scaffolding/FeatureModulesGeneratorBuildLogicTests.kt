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
                orgSegment = "acme"
            )
        }

        val buildLogicDir = projectRootVf.findChild("build-logic") ?: error("build-logic not created")
        // check build.gradle.kts exists
        val buildGradle = buildLogicDir.findChild("build.gradle.kts") ?: error("build.gradle.kts missing")
        val buildText = VfsUtil.loadText(buildGradle)
        // should contain registrations for data, di, domain, presentation
        assertTrue(buildText.contains("com.acme.convention.DataConventionPlugin") || buildText.contains("DataConventionPlugin"))
        assertTrue(buildText.contains("com.acme.convention.DiConventionPlugin") || buildText.contains("DiConventionPlugin"))
        assertTrue(buildText.contains("com.acme.convention.DomainConventionPlugin") || buildText.contains("DomainConventionPlugin"))
        assertTrue(buildText.contains("com.acme.convention.PresentationConventionPlugin") || buildText.contains("PresentationConventionPlugin"))

        // check convention Kotlin files exist and have correct package line
        val srcMain = VfsUtil.findRelativeFile("build-logic/src/main/kotlin/com/acme/convention", projectRootVf)
            ?: error("convention src dir missing")
        listOf("DataConventionPlugin.kt", "DIConventionPlugin.kt", "DomainConventionPlugin.kt", "PresentationConventionPlugin.kt").forEach { f ->
            val vf = srcMain.findChild(f) ?: error("$f missing in build-logic")
            val text = VfsUtil.loadText(vf)
            assertTrue(text.contains("package com.acme.convention"))
            // Should import the helper when using configureAndroidLibraryDefaults()
            if (text.contains("configureAndroidLibraryDefaults()")) {
                assertTrue(text.contains("import com.acme.convention.helpers.configureAndroidLibraryDefaults"))
            }
        }

        // helpers directory and files should exist with correct package
        val helpersDir = srcMain.findChild("helpers") ?: error("helpers directory missing under convention")
        val helperFiles = listOf(
            "AndroidLibraryDefaults.kt",
            "TestOptions.kt",
            "StandardTestDependencies.kt"
        )
        helperFiles.forEach { hf ->
            val vf = helpersDir.findChild(hf) ?: error("$hf missing in helpers")
            val text = VfsUtil.loadText(vf)
            assertTrue(text.contains("package com.acme.convention.helpers"))
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

    fun testHelperImportUsesPackageReplacementOrg() {
        val tempProject = Files.createTempDirectory("fm-gen-build-logic-helper").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "catalog",
                includePresentation = false,
                includeDatasource = true,
                datasourceCombined = false,
                datasourceRemote = true,
                datasourceLocal = false,
                includeDi = false,
                orgSegment = "acme"
            )
        }

        val srcMain = VfsUtil.findRelativeFile("build-logic/src/main/kotlin/com/acme/convention", projectRootVf)
            ?: error("convention src dir missing")
        val remote = srcMain.findChild("RemoteDataSourceConventionPlugin.kt") ?: error("RemoteDataSourceConventionPlugin.kt missing")
        val text = VfsUtil.loadText(remote)
        // Package should use provided org segment
        assertTrue(text.contains("package com.acme.convention"))
        // Helper import must use the provided org segment via PACKAGE replacement
        assertTrue(text.contains("import com.acme.convention.helpers.configureAndroidLibraryDefaults"))
        // ensure no other organization appears
        assertFalse(Regex("com\\.[a-zA-Z0-9_.]+\\.convention").findAll(text).any { it.value != "com.acme.convention" })
    }

    fun testNoPackageTokensRemainInGeneratedBuildLogic() {
        val tempProject = Files.createTempDirectory("fm-gen-build-logic-no-tokens").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "alpha",
                includePresentation = true,
                includeDatasource = true,
                datasourceCombined = false,
                datasourceRemote = true,
                datasourceLocal = true,
                includeDi = true,
                orgSegment = "acme"
            )
        }

        val buildLogic = VfsUtil.findRelativeFile("build-logic", projectRootVf) ?: error("build-logic missing")
        // scan several expected files
        val filesToCheck = listOf(
            "build.gradle.kts",
            "src/main/kotlin/com/acme/convention/DataConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/DIConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/DomainConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/PresentationConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/DataSourceConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/RemoteDataSourceConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/LocalDataSourceConventionPlugin.kt",
            "src/main/kotlin/com/acme/convention/helpers/AndroidLibraryDefaults.kt",
            "src/main/kotlin/com/acme/convention/helpers/TestOptions.kt",
            "src/main/kotlin/com/acme/convention/helpers/StandardTestDependencies.kt",
        )
        filesToCheck.forEach { relPath ->
            val vf = VfsUtil.findRelativeFile(relPath, buildLogic) ?: return@forEach
            val text = VfsUtil.loadText(vf)
            assertFalse(text.contains("PACKAGE"))
            assertFalse(text.contains("${'$'}{PACKAGE}"))
        }
    }

    fun testRemoteAndLocalDatasourcePluginsWhenEnabledSeparately() {
        val tempProject = Files.createTempDirectory("fm-gen-build-logic-ds-separate").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "beta",
                includePresentation = false,
                includeDatasource = true,
                datasourceCombined = false,
                datasourceRemote = true,
                datasourceLocal = true,
                includeDi = false,
                orgSegment = "acme"
            )
        }

        val srcMain = VfsUtil.findRelativeFile("build-logic/src/main/kotlin/com/acme/convention", projectRootVf)
            ?: error("convention src dir missing")
        assertNull(srcMain.findChild("DataSourceConventionPlugin.kt"))
        assertNotNull(srcMain.findChild("RemoteDataSourceConventionPlugin.kt"))
        assertNotNull(srcMain.findChild("LocalDataSourceConventionPlugin.kt"))
    }
}

