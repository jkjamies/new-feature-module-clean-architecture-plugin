package com.github.jkjamies.cammp.feature.usecasegenerator.scaffolding

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

class UseCaseGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesUseCaseInDomain() {
        val tempDir = Files.createTempDirectory("usecase-generator-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/domain layout
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "checkout")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
        }

        val generator = UseCaseGenerator(project)
        val result = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/checkout/domain",
                useCaseName = "PlaceOrderUseCase",
                diEnabled = true,
                useHilt = true,
                koinAnnotations = false
            )
        }
        assertTrue(result.contains("UseCase 'PlaceOrderUseCase' generated"))

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/checkout/domain/src/main/kotlin/com/jkjamies/features/checkout/domain/usecase/PlaceOrderUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text = VfsUtilCore.loadText(useCaseFile!!)
        assertTrue(text.contains("package com.jkjamies.features.checkout.domain.usecase"))
        assertTrue(text.contains("class PlaceOrderUseCase"))
        assertTrue(text.contains("@Inject constructor"))
        assertTrue(text.contains("suspend operator fun invoke()"))
        assertFalse("Unreplaced placeholders in use case", text.contains("${'$'}{"))
    }

    fun testGenerateWritesTodoWhenNotHilt() {
        val tempDir = Files.createTempDirectory("usecase-generator-test-nohilt").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/domain layout
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "payments")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/payments/domain",
                useCaseName = "SyncInvoicesUseCase",
                diEnabled = true,
                useHilt = false, // not Hilt
                koinAnnotations = true
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/payments/domain/src/main/kotlin/com/jkjamies/features/payments/domain/usecase/SyncInvoicesUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text = VfsUtilCore.loadText(useCaseFile!!).trim()
        assertEquals("// TODO: not yet available", text)
    }

    fun testGenerateAddsSelectedRepositoriesToConstructor() {
        val tempDir = Files.createTempDirectory("usecase-generator-test-withrepos").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/domain layout and a repository interface
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "orders")
                ?: error("Failed to create feature dir")
            val domain = VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
            val repoPkg = VfsUtil.createDirectoryIfMissing(domain, "src/main/kotlin/com/jkjamies/features/orders/domain/repository")
                ?: error("Failed to create repo package")
            val repoFile = repoPkg.createChildData(this, "OrderRepository.kt")
            VfsUtil.saveText(repoFile, "package com.jkjamies.features.orders.domain.repository\n\ninterface OrderRepository\n")
            domain
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/orders/domain",
                useCaseName = "FetchOrdersUseCase",
                diEnabled = true,
                useHilt = true,
                koinAnnotations = false,
                selectedRepositories = listOf("OrderRepository")
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/orders/domain/src/main/kotlin/com/jkjamies/features/orders/domain/usecase/FetchOrdersUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text2 = VfsUtilCore.loadText(useCaseFile!!)
        // Import present
        assertTrue(text2.contains("import com.jkjamies.features.orders.domain.repository.OrderRepository"))
        // Constructor parameter present (lower camel name)
        assertTrue(text2.contains("private val orderRepository: OrderRepository"))
        // Class header still valid
        assertTrue(text2.contains("class FetchOrdersUseCase @Inject constructor("))
        assertFalse("Unreplaced placeholders in use case", text2.contains("${'$'}{"))
    }

    fun testGenerateHandlesFqnRepositorySelection() {
        val tempDir = Files.createTempDirectory("usecase-generator-test-fqn").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        // Create feature/domain layout and a repository interface
        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "catalog")
                ?: error("Failed to create feature dir")
            val domain = VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
            val repoPkg = VfsUtil.createDirectoryIfMissing(domain, "src/main/kotlin/com/jkjamies/features/catalog/domain/repository")
                ?: error("Failed to create repo package")
            val repoFile = repoPkg.createChildData(this, "CatalogRepository.kt")
            VfsUtil.saveText(repoFile, "package com.jkjamies.features.catalog.domain.repository\n\ninterface CatalogRepository\n")
            domain
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/catalog/domain",
                useCaseName = "LoadCatalog",
                diEnabled = true,
                useHilt = true,
                koinAnnotations = false,
                selectedRepositories = listOf("com.jkjamies.features.catalog.domain.repository.CatalogRepository")
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/catalog/domain/src/main/kotlin/com/jkjamies/features/catalog/domain/usecase/LoadCatalogUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text3 = VfsUtilCore.loadText(useCaseFile!!)
        assertTrue(text3.contains("import com.jkjamies.features.catalog.domain.repository.CatalogRepository"))
        assertTrue(text3.contains("private val catalogRepository: CatalogRepository"))
    }

    fun testGenerateAddsMultipleRepositoriesWithFqnAndSimple() {
        val tempDir = Files.createTempDirectory("usecase-gen-multi-repos").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "billing")
                ?: error("Failed to create feature dir")
            val domain = VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
            val repoPkg = VfsUtil.createDirectoryIfMissing(domain, "src/main/kotlin/com/jkjamies/features/billing/domain/repository")
                ?: error("Failed to create repo package")
            val repo1 = repoPkg.createChildData(this, "InvoiceRepository.kt")
            VfsUtil.saveText(repo1, "package com.jkjamies.features.billing.domain.repository\n\ninterface InvoiceRepository\n")
            domain
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/billing/domain",
                useCaseName = "GenerateStatement",
                diEnabled = true,
                useHilt = true,
                selectedRepositories = listOf(
                    "InvoiceRepository",
                    "com.jkjamies.features.billing.domain.repository.StatementRepository"
                )
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/billing/domain/src/main/kotlin/com/jkjamies/features/billing/domain/usecase/GenerateStatementUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text4 = VfsUtilCore.loadText(useCaseFile!!)
        assertTrue(text4.contains("import com.jkjamies.features.billing.domain.repository.InvoiceRepository"))
        assertTrue(text4.contains("import com.jkjamies.features.billing.domain.repository.StatementRepository"))
        assertTrue(text4.contains("private val invoiceRepository: InvoiceRepository"))
        assertTrue(text4.contains("private val statementRepository: StatementRepository"))
        assertFalse("Unreplaced placeholders in use case", text4.contains("${'$'}{"))
    }

    fun testGenerateAppendsSuffixWhenMissing() {
        val tempDir = Files.createTempDirectory("usecase-gen-suffix").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "profile")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/profile/domain",
                useCaseName = "SyncSettings",
                diEnabled = true,
                useHilt = true
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/profile/domain/src/main/kotlin/com/jkjamies/features/profile/domain/usecase/SyncSettingsUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text3 = VfsUtilCore.loadText(useCaseFile!!)
        assertTrue(text3.contains("class SyncSettingsUseCase"))
        assertFalse("Unreplaced placeholders in use case", text3.contains("${'$'}{"))
    }

    fun testGenerateDetectsOrgFromSiblingIfMissingInDomain() {
        val tempDir = Files.createTempDirectory("usecase-gen-org-detect").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        WriteAction.compute<VirtualFile, RuntimeException> {
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "settings")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "domain")
                ?: error("Failed to create domain dir")
            val data = VfsUtil.createDirectoryIfMissing(feature, "data")
                ?: error("Failed to create data dir")
            VfsUtil.createDirectoryIfMissing(data, "src/main/kotlin/com/acme")
                ?: error("Failed to create sibling org path")
        }

        val generator = UseCaseGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                domainModuleDirRelativeToProject = "features/settings/domain",
                useCaseName = "LoadPrefs",
                diEnabled = true,
                useHilt = true
            )
        }

        val useCaseFile = VfsUtil.findRelativeFile(
            "features/settings/domain/src/main/kotlin/com/acme/features/settings/domain/usecase/LoadPrefsUseCase.kt",
            vfsRoot
        )
        assertNotNull(useCaseFile)
        val text = VfsUtilCore.loadText(useCaseFile!!)
        assertTrue(text.contains("package com.acme.features.settings.domain.usecase"))
    }
}
