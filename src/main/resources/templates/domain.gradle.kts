plugins {
    val serialization = libs.plugins.serialization.get()
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.google.ksp)
    kotlin(serialization.pluginId) version serialization.version.requiredVersion
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "NAMESPACE"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "imgurBaseUrl", "\"https://api.imgur.com/\"")
        buildConfigField("String", "imgurSearchUrl", "\"3/gallery/search/\"")
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
    buildFeatures {
        buildConfig = true
    }
}

sqldelight {
    databases {
        create("ImgurDatabase") {
            packageName.set("com.jkjamies.imgur.api")
        }
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
    // Modules
    implementation(project(":core"))

    // KMP-Friendly
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization)
    implementation(libs.sqldelight.adapters)
    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    // Koin Annotations
    implementation(platform(libs.koin.annotations.bom))
    implementation(libs.koin.annotations)
    implementation(libs.koin.ksp.compiler)
    ksp(platform(libs.koin.annotations.bom))
    ksp(libs.koin.ksp.compiler)
    // Kermit
    implementation(libs.kermit)
    implementation(libs.kermit.koin)

    // Android
    // SQLDelight
    implementation(libs.android.sqldelight.driver)
    implementation(libs.koin.android)

    // Test
    testImplementation(libs.junit.kotest)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
}
