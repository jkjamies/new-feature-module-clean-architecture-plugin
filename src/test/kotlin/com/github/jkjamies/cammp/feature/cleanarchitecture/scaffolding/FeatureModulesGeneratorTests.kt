package com.github.jkjamies.cammp.feature.cleanarchitecture.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * End-to-end tests for [FeatureModulesGenerator] integrating with VFS (LocalFileSystem, VfsUtil).
 */
class FeatureModulesGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesModulesAndUpdatesSettings() {
        val tempProject = Files.createTempDirectory("feature-generator-test").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        // generation touches VFS; run in write context
        val message = WriteAction.compute<String, RuntimeException> {
            generator.generate(projectRootVf.path, "features", "payments")
        }
        assertTrue(message.contains("updated settings.gradle"))

        val featureDir = VfsUtil.findRelativeFile("features/payments", projectRootVf)
            ?: error("Feature dir not created")

        listOf("domain", "data", "di", "presentation").forEach { m ->
            assertNotNull("Module $m missing", featureDir.findChild(m))
        }

        val settingsFile = projectRootVf.findChild("settings.gradle.kts")
            ?: error("settings file not created")
        val content = VfsUtil.loadText(settingsFile)
        listOf("domain", "data", "di", "presentation").forEach { m ->
            val line = "include(\":features:payments:$m\")"
            assertTrue("Missing include for $m", content.contains(line))
        }
    }

    fun testGenerateWithoutPresentationExcludesPresentationModule() {
        val tempProject = Files.createTempDirectory("feature-generator-test-nopres").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(projectRootVf.path, "features", "orders", includePresentation = false)
        }

        val featureDir = VfsUtil.findRelativeFile("features/orders", projectRootVf)
            ?: error("Feature dir not created")

        // should have base modules
        listOf("domain", "data", "di").forEach { m ->
            assertNotNull("Module $m missing", featureDir.findChild(m))
        }
        // and not have presentation
        assertNull("Presentation module should not be created", featureDir.findChild("presentation"))

        val settingsFile = projectRootVf.findChild("settings.gradle.kts")
            ?: error("settings file not created")
        val content = VfsUtil.loadText(settingsFile)
        // ensure includes only for the base modules
        listOf("domain", "data", "di").forEach { m ->
            val line = "include(\":features:orders:$m\")"
            assertTrue("Missing include for $m", content.contains(line))
        }
        assertFalse("Include for presentation should be absent", content.contains("include(\":features:orders:presentation\")"))
    }

    fun testGenerateWithCombinedDatasourceCreatesDataSourceModule() {
        val tempProject = Files.createTempDirectory("feature-generator-test-combined").toFile()
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
                datasourceLocal = false
            )
        }

        val featureDir = VfsUtil.findRelativeFile("features/catalog", projectRootVf)
            ?: error("Feature dir not created")

        // base modules
        listOf("domain", "data", "di", "presentation").forEach { m ->
            assertNotNull("Module $m missing", featureDir.findChild(m))
        }
        // combined datasource module
        assertNotNull("dataSource module missing", featureDir.findChild("dataSource"))
        assertNull("remoteDataSource should not exist", featureDir.findChild("remoteDataSource"))
        assertNull("localDataSource should not exist", featureDir.findChild("localDataSource"))

        // verify data module contains dataSource subdirectory when combined selected
        val dataModule = featureDir.findChild("data") ?: error("data module not found")
        val dataPkgPath = "src/main/kotlin/com/jkjamies/features/catalog/data"
        val dataPkgDir = VfsUtil.findRelativeFile(dataPkgPath, dataModule) ?: error("data package dir not created")
        assertNotNull("dataSource subdir in data module missing", dataPkgDir.findChild("dataSource"))
        assertNull(dataPkgDir.findChild("remoteDataSource"))
        assertNull(dataPkgDir.findChild("localDataSource"))

        val settingsFile = projectRootVf.findChild("settings.gradle.kts") ?: error("settings file not created")
        val content = VfsUtil.loadText(settingsFile)
        val expectedIncludes = listOf("domain", "data", "di", "presentation", "dataSource")
        expectedIncludes.forEach { m ->
            val line = "include(\":features:catalog:$m\")"
            assertTrue("Missing include for $m", content.contains(line))
        }
    }

    fun testGenerateWithRemoteAndLocalDatasourceCreatesBothSpecificModules() {
        val tempProject = Files.createTempDirectory("feature-generator-test-remote-local").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "account",
                includePresentation = false,
                includeDatasource = true,
                datasourceCombined = false,
                datasourceRemote = true,
                datasourceLocal = true
            )
        }

        val featureDir = VfsUtil.findRelativeFile("features/account", projectRootVf)
            ?: error("Feature dir not created")

        // base modules
        listOf("domain", "data", "di").forEach { m ->
            assertNotNull("Module $m missing", featureDir.findChild(m))
        }
        // specific datasource modules
        assertNotNull("remoteDataSource module missing", featureDir.findChild("remoteDataSource"))
        assertNotNull("localDataSource module missing", featureDir.findChild("localDataSource"))
        assertNull("dataSource (combined) should not exist", featureDir.findChild("dataSource"))

        // verify data module contains remoteDataSource and localDataSource subdirectories when both selected
        val dataModule = featureDir.findChild("data") ?: error("data module not found")
        val dataPkgPath = "src/main/kotlin/com/jkjamies/features/account/data"
        val dataPkgDir = VfsUtil.findRelativeFile(dataPkgPath, dataModule) ?: error("data package dir not created")
        assertNotNull("remoteDataSource subdir in data module missing", dataPkgDir.findChild("remoteDataSource"))
        assertNotNull("localDataSource subdir in data module missing", dataPkgDir.findChild("localDataSource"))
        assertNull(dataPkgDir.findChild("dataSource"))

        val settingsFile = projectRootVf.findChild("settings.gradle.kts") ?: error("settings file not created")
        val content = VfsUtil.loadText(settingsFile)
        listOf("domain", "data", "di", "remoteDataSource", "localDataSource").forEach { m ->
            val line = "include(\":features:account:$m\")"
            assertTrue("Missing include for $m", content.contains(line))
        }
        assertFalse("Include for presentation should be absent", content.contains("include(\":features:account:presentation\")"))
    }

    fun testGenerateWithRemoteOnlyCreatesRemoteDataSourceModule() {
        val tempProject = Files.createTempDirectory("feature-generator-test-remote-only").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "search",
                includePresentation = true,
                includeDatasource = true,
                datasourceCombined = false,
                datasourceRemote = true,
                datasourceLocal = false
            )
        }

        val featureDir = VfsUtil.findRelativeFile("features/search", projectRootVf)
            ?: error("Feature dir not created")

        assertNotNull(featureDir.findChild("remoteDataSource"))
        assertNull(featureDir.findChild("localDataSource"))
        assertNull(featureDir.findChild("dataSource"))

        // verify data module contains remoteDataSource subdirectory when only remote selected
        val dataModuleRemote = featureDir.findChild("data") ?: error("data module not found")
        val dataPkgPathRemote = "src/main/kotlin/com/jkjamies/features/search/data"
        val dataPkgDirRemote = VfsUtil.findRelativeFile(dataPkgPathRemote, dataModuleRemote) ?: error("data package dir not created")
        assertNotNull("remoteDataSource subdir in data module missing", dataPkgDirRemote.findChild("remoteDataSource"))
        assertNull(dataPkgDirRemote.findChild("localDataSource"))
        assertNull(dataPkgDirRemote.findChild("dataSource"))

        val content = VfsUtil.loadText(projectRootVf.findChild("settings.gradle.kts")!!)
        assertTrue(content.contains("include(\":features:search:remoteDataSource\")"))
        assertFalse(content.contains("include(\":features:search:localDataSource\")"))
        assertFalse(content.contains("include(\":features:search:dataSource\")"))
    }

    fun testGenerateWithLocalOnlyCreatesLocalDataSourceModule() {
        val tempProject = Files.createTempDirectory("feature-generator-test-local-only").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "prefs",
                includePresentation = false,
                includeDatasource = true,
                datasourceCombined = false,
                datasourceRemote = false,
                datasourceLocal = true
            )
        }

        val featureDir = VfsUtil.findRelativeFile("features/prefs", projectRootVf)
            ?: error("Feature dir not created")

        assertNotNull(featureDir.findChild("localDataSource"))
        assertNull(featureDir.findChild("remoteDataSource"))
        assertNull(featureDir.findChild("dataSource"))

        // verify data module contains localDataSource subdirectory when only local selected
        val dataModuleLocal = featureDir.findChild("data") ?: error("data module not found")
        val dataPkgPathLocal = "src/main/kotlin/com/jkjamies/features/prefs/data"
        val dataPkgDirLocal = VfsUtil.findRelativeFile(dataPkgPathLocal, dataModuleLocal) ?: error("data package dir not created")
        assertNotNull("localDataSource subdir in data module missing", dataPkgDirLocal.findChild("localDataSource"))
        assertNull(dataPkgDirLocal.findChild("remoteDataSource"))
        assertNull(dataPkgDirLocal.findChild("dataSource"))

        val content = VfsUtil.loadText(projectRootVf.findChild("settings.gradle.kts")!!)
        assertTrue(content.contains("include(\":features:prefs:localDataSource\")"))
        assertFalse(content.contains("include(\":features:prefs:remoteDataSource\")"))
        assertFalse(content.contains("include(\":features:prefs:dataSource\")"))
    }

    fun testGenerateWithoutDiExcludesDiModule() {
        val tempProject = Files.createTempDirectory("feature-generator-test-nodi").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val generator = FeatureModulesGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectRootVf.path,
                "features",
                "billing",
                includePresentation = true,
                includeDatasource = false,
                datasourceCombined = false,
                datasourceRemote = false,
                datasourceLocal = false,
                includeDi = false
            )
        }

        val featureDir = VfsUtil.findRelativeFile("features/billing", projectRootVf)
            ?: error("Feature dir not created")

        // base modules except di
        assertNotNull("domain module missing", featureDir.findChild("domain"))
        assertNotNull("data module missing", featureDir.findChild("data"))
        assertNull("di module should not be created", featureDir.findChild("di"))
        // presentation included per flag
        assertNotNull("presentation module missing", featureDir.findChild("presentation"))

        val settingsFile = projectRootVf.findChild("settings.gradle.kts") ?: error("settings file not created")
        val content = VfsUtil.loadText(settingsFile)
        assertTrue(content.contains("include(\":features:billing:domain\")"))
        assertTrue(content.contains("include(\":features:billing:data\")"))
        assertTrue(content.contains("include(\":features:billing:presentation\")"))
        assertFalse(content.contains("include(\":features:billing:di\")"))
    }

}
