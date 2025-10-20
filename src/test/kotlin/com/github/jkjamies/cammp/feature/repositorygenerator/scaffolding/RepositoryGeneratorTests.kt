package com.github.jkjamies.cammp.feature.repositorygenerator.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Verifies [RepositoryGenerator] creates expected files via VfsUtil.
 */
class RepositoryGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesDomainDataAndHiltModuleWhenEnabled() {
        val tempDir = Files.createTempDirectory("repo-generator-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/data layout
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "catalog")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "data")
                ?: error("Failed to create data dir")
        }

        val generator = RepositoryGenerator(project)
        val result = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                dataModuleDirRelativeToProject = "features/catalog/data",
                repositoryName = "ProductRepository",
                diEnabled = true,
                useHilt = true,
                koinAnnotations = false
            )
        }
        assertTrue(result.contains("Repository 'ProductRepository' generated"))
        assertTrue(result.contains("Hilt module created"))

        val domainRepoFile = VfsUtil.findRelativeFile(
            "features/catalog/domain/src/main/kotlin/com/jkjamies/features/catalog/domain/repository/ProductRepository.kt",
            vfsRoot
        )
        assertNotNull(domainRepoFile)

        val dataRepoFile = VfsUtil.findRelativeFile(
            "features/catalog/data/src/main/kotlin/com/jkjamies/features/catalog/data/repository/ProductRepositoryImpl.kt",
            vfsRoot
        )
        assertNotNull(dataRepoFile)

        val diModuleFile = VfsUtil.findRelativeFile(
            "features/catalog/di/src/main/kotlin/com/jkjamies/features/catalog/di/repository/RepositoryModule.kt",
            vfsRoot
        )
        assertNotNull(diModuleFile)
        val text = VfsUtilCore.loadText(diModuleFile!!)
        assertTrue(text.contains("@Module"))
        assertTrue(text.contains("@InstallIn(SingletonComponent::class)"))
        assertTrue(text.contains("abstract class RepositoryModule"))
        assertTrue(text.contains("@Binds"))
        assertTrue(text.contains("abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository"))
    }

    fun testNoDiArtifactsWhenDisabled() {
        val tempDir = Files.createTempDirectory("repo-generator-test2").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "search")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "data")
                ?: error("Failed to create data dir")
        }

        val generator = RepositoryGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                dataModuleDirRelativeToProject = "features/search/data",
                repositoryName = "SearchRepository",
                diEnabled = false,
                useHilt = true,
                koinAnnotations = false
            )
        }

        // DI module should not exist
        val diModuleFile = VfsUtil.findRelativeFile(
            "features/search/di/src/main/kotlin/com/jkjamies/features/search/di/repository/RepositoryModule.kt",
            vfsRoot
        )
        assertNull(diModuleFile)
    }
}
