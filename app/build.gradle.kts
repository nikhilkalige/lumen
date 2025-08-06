import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt) // Apply Hilt plugin
    kotlin("kapt") // Add kapt for Hilt annotation processing
}

android {
    namespace = "com.benki.lumen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.benki.lumen"
        minSdk = 36
        targetSdk = 36
        versionCode = 17
        versionName = "17.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Or your desired Java version
        targetCompatibility = JavaVersion.VERSION_11 // Or your desired Java version
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/INDEX.LIST",   // your existing exclude
                "/META-INF/DEPENDENCIES"  // add this!
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Compose BOM
    implementation(platform(libs.compose.bom))
    // Compose libraries
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.compose.navigation)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.compose.material.icons.extended)

    // Architecture components
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.viewmodel.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Credential Manager (OAuth2)
    implementation(libs.credentials)

    // Networking (Ktor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.google.api.client)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.services.oauth2)
    implementation(libs.androidx.material.icons.extended.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.coroutines.play.services) // Or the latest version
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
