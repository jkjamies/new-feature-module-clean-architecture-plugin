import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    `kotlin-dsl`
}

// Add AGP to classpath so we can reference Android DSL types in convention plugins
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val agpVersion = libs.findVersion("agp").get().requiredVersion
val kotlinVersion = libs.findVersion("kotlin").get().requiredVersion

dependencies {
    implementation("com.android.tools.build:gradle:$agpVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

gradlePlugin {
    plugins {
        // New universal IDs
        register("androidLibraryDataConvention") {
            id = "com.PACKAGE.convention.android.library.data"
            implementationClass = "com.PACKAGE.convention.DataConventionPlugin"
        }
        register("androidLibraryDiConvention") {
            id = "com.PACKAGE.convention.android.library.di"
            implementationClass = "com.PACKAGE.convention.DiConventionPlugin"
        }
        register("androidLibraryDomainConvention") {
            id = "com.PACKAGE.convention.android.library.domain"
            implementationClass = "com.PACKAGE.convention.DomainConventionPlugin"
        }
        register("androidLibraryPresentationConvention") {
            id = "com.PACKAGE.convention.android.library.presentation"
            implementationClass = "com.PACKAGE.convention.PresentationConventionPlugin"
        }
    }
}
