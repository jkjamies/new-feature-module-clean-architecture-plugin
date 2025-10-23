package com.github.jkjamies.cammp.feature.presentationgenerator.scaffolding

import com.github.jkjamies.cammp.feature.presentationgenerator.ui.GenerateScreenDialog
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import java.nio.file.Files

/**
 * Verifies [PresentationScreenGenerator] creates expected directories and files via VfsUtil.
 */
class PresentationScreenGeneratorTests : LightPlatformTestCase() {
    fun testGenerateCreatesScreenDirectoryAndFiles() {
        val tempDir = Files.createTempDirectory("presentation-generator-test").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        val presentationDir = WriteAction.compute<VirtualFile, RuntimeException> {
            // simulate feature structure: features/payments/presentation
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "payments")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "presentation")
                ?: error("Failed to create presentation dir")
        }

        val generator = PresentationScreenGenerator(project)
        val result = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                targetDirRelativeToProject = "features/payments/presentation",
                screenName = "Checkout",
                addNavigation = true,
                useFlowStateHolder = true,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVI
            )
        }
        assertTrue(result.contains("Presentation screen 'Checkout'"))

        // Base package directory: src/main/kotlin/com/jkjamies/features/payments/presentation
        val basePkgDir = VfsUtil.findRelativeFile("src/main/kotlin/com/jkjamies/features/payments/presentation", presentationDir)
            ?: error("base package dir not created")

        val screenDir = VfsUtil.findRelativeFile("checkout", basePkgDir) ?: error("screen dir not created")
        assertNotNull(screenDir.findChild("CheckoutScreen.kt"))
        assertNotNull(screenDir.findChild("CheckoutIntent.kt"))
        assertNotNull(screenDir.findChild("CheckoutViewModel.kt"))
        assertNotNull(screenDir.findChild("CheckoutUiState.kt"))
        assertNotNull(screenDir.findChild("CheckoutStateHolder.kt"))

        // Navigation artifacts
        val navDir = VfsUtil.findRelativeFile("navigation", basePkgDir) ?: error("navigation dir not created")
        assertNotNull(navDir.findChild("PaymentsNavHost.kt"))
        val destDir = VfsUtil.findRelativeFile("destinations", navDir) ?: error("destinations dir not created")
        assertNotNull(destDir.findChild("CheckoutDestination.kt"))

        // FlowStateHolder should be created inside the base package directory, with Capitalized parent name prefix
        assertNotNull(basePkgDir.findChild("PaymentsFlowStateHolder.kt"))
    }

    fun testDirectoryOmitsScreenSuffix() {
        val tempDir = Files.createTempDirectory("presentation-generator-test2").toFile()
        val vfsRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
            ?: error("Failed to get VFS for temp dir")

        val presentationDir = WriteAction.compute<VirtualFile, RuntimeException> {
            // simulate feature structure: features/home/presentation
            val features = VfsUtil.createDirectoryIfMissing(vfsRoot, "features")
                ?: error("Failed to create features dir")
            val feature = VfsUtil.createDirectoryIfMissing(features, "home")
                ?: error("Failed to create feature dir")
            VfsUtil.createDirectoryIfMissing(feature, "presentation")
                ?: error("Failed to create presentation dir")
        }

        val generator = PresentationScreenGenerator(project)
        WriteAction.run<RuntimeException> {
            generator.generate(
                projectBasePath = vfsRoot.path,
                targetDirRelativeToProject = "features/home/presentation",
                screenName = "HomeScreen",
                addNavigation = false,
                useFlowStateHolder = false,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVI
            )
        }

        val basePkgDir = VfsUtil.findRelativeFile("src/main/kotlin/com/jkjamies/features/home/presentation", presentationDir)
            ?: error("base package dir not created")
        val screenDir = VfsUtil.findRelativeFile("home", basePkgDir) ?: error("screen dir 'home' not created")
        // Main screen file keeps original name
        assertNotNull(screenDir.findChild("HomeScreen.kt"))
        // Ancillary files drop the trailing Screen already
        assertNotNull(screenDir.findChild("HomeIntent.kt"))
        assertNotNull(screenDir.findChild("HomeViewModel.kt"))
        assertNotNull(screenDir.findChild("HomeUiState.kt"))
        assertNotNull(screenDir.findChild("HomeScreenStateHolder.kt") ?: screenDir.findChild("HomeStateHolder.kt"))
    }

    fun testGeneratesScreenWithReplacedPlaceholders_MVVM() {
        val tempProject = Files.createTempDirectory("presentation-gen-test").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        // Create presentation module directory structure under write action
        val presDir = WriteAction.compute<VirtualFile, RuntimeException> {
            VfsUtil.createDirectoryIfMissing(projectRootVf, "features/profile/presentation")
                ?: error("Failed to create presentation module dir")
        }

        val generator = PresentationScreenGenerator(project)
        val resultMsg = WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = projectRootVf.path,
                targetDirRelativeToProject = "features/profile/presentation",
                screenName = "HomeScreen",
                addNavigation = false,
                useFlowStateHolder = false,
                useScreenStateHolder = false,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVVM
            )
        }
        assertTrue(resultMsg.contains("Presentation screen 'HomeScreen'"))

        // Verify main Screen.kt created with placeholders replaced
        val screenFile = VfsUtil.findRelativeFile(
            "src/main/kotlin/com/jkjamies/features/profile/presentation/home/HomeScreen.kt",
            presDir
        ) ?: error("HomeScreen.kt not created")
        val content = VfsUtil.loadText(screenFile)
        assertTrue(content.contains("package com.jkjamies.features.profile.presentation.home"))
        assertTrue(content.contains("fun HomeScreen()"))
        assertFalse("Placeholders were not replaced", content.contains("\${"))
    }

    fun testGeneratesNavigationAndDestinationWithReplacedPlaceholders() {
        val tempProject = Files.createTempDirectory("presentation-gen-test-nav").toFile()
        val projectRootVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempProject)
            ?: error("project root VFS not found")

        val presDir = WriteAction.compute<VirtualFile, RuntimeException> {
            VfsUtil.createDirectoryIfMissing(projectRootVf, "features/orders/presentation")
                ?: error("Failed to create presentation module dir")
        }

        val generator = PresentationScreenGenerator(project)
        WriteAction.compute<String, RuntimeException> {
            generator.generate(
                projectBasePath = projectRootVf.path,
                targetDirRelativeToProject = "features/orders/presentation",
                screenName = "DetailsScreen",
                addNavigation = true,
                useFlowStateHolder = true,
                useScreenStateHolder = true,
                diChoice = GenerateScreenDialog.DiChoice.HILT,
                koinAnnotations = false,
                patternChoice = GenerateScreenDialog.PatternChoice.MVI
            )
        }

        // NavHost
        val navHost = VfsUtil.findRelativeFile(
            "src/main/kotlin/com/jkjamies/features/orders/presentation/navigation/OrdersNavHost.kt",
            presDir
        ) ?: error("NavHost not created")
        val navHostText = VfsUtil.loadText(navHost)
        assertTrue(navHostText.contains("package com.jkjamies.features.orders.presentation.navigation"))
        assertTrue(navHostText.contains("fun OrdersNavHost()"))
        assertFalse(navHostText.contains("\${"))

        // Destination
        val destination = VfsUtil.findRelativeFile(
            "src/main/kotlin/com/jkjamies/features/orders/presentation/navigation/destinations/DetailsScreenDestination.kt",
            presDir
        ) ?: error("Destination not created")
        val destText = VfsUtil.loadText(destination)
        assertTrue(destText.contains("package com.jkjamies.features.orders.presentation.navigation.destinations"))
        assertTrue(destText.contains("object DetailsScreenDestination"))
        assertTrue(destText.contains("const val route = \"detailsscreen\""))
        assertFalse(destText.contains("\${"))

        // Intent for MVI pattern
        val intent = VfsUtil.findRelativeFile(
            "src/main/kotlin/com/jkjamies/features/orders/presentation/details/DetailsIntent.kt",
            presDir
        ) ?: error("Intent not created for MVI")
        val intentText = VfsUtil.loadText(intent)
        assertTrue(intentText.contains("sealed interface DetailsIntent"))
        assertFalse(intentText.contains("\${"))

        // ScreenStateHolder should be created under screen folder and be replaced
        val ssh = VfsUtil.findRelativeFile(
            "src/main/kotlin/com/jkjamies/features/orders/presentation/details/DetailsScreenStateHolder.kt",
            presDir
        ) ?: error("ScreenStateHolder not created")
        val sshText = VfsUtil.loadText(ssh)
        assertTrue(sshText.contains("class DetailsScreenStateHolder"))
        assertFalse(sshText.contains("\${"))

        // FlowStateHolder created at base package level
        val fsh = VfsUtil.findRelativeFile(
            "src/main/kotlin/com/jkjamies/features/orders/presentation/OrdersFlowStateHolder.kt",
            presDir
        ) ?: error("FlowStateHolder not created")
        val fshText = VfsUtil.loadText(fsh)
        assertTrue(fshText.contains("class OrdersFlowStateHolder"))
        assertFalse(fshText.contains("\${"))
    }
}
