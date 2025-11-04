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
    // Test
    testImplementation(libs.junit.kotest)
}