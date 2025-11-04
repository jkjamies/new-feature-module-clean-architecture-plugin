plugins {
    val serialization = libs.plugins.serialization.get()
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.google.ksp)
    kotlin(serialization.pluginId) version serialization.version.requiredVersion
}

android {
    namespace = "NAMESPACE"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.dokkaHtml {
    dokkaSourceSets.configureEach {
        includeNonPublic = true
    }
}

android.testOptions {
    unitTests.all {
        it.useJUnitPlatform()
    }
}

dependencies {
    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    // Koin Annotations
    implementation(platform(libs.koin.annotations.bom))
    implementation(libs.koin.annotations)
    implementation(libs.koin.ksp.compiler)
    ksp(platform(libs.koin.annotations.bom))
    ksp(libs.koin.ksp.compiler)

    // Test
    testImplementation(libs.junit.kotest)
}
