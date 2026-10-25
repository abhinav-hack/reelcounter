import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        FileInputStream(keystorePropertiesFile).use(::load)
    }
}

fun signingProperty(name: String): String? =
    keystoreProperties.getProperty(name) ?: providers.gradleProperty(name).orNull

android {
    namespace = "com.reelcounter"
    compileSdk = 36

    signingConfigs {
        create("release") {
            val storeFilePath = signingProperty("reelCounterReleaseStoreFile")
                ?: error("Missing reelCounterReleaseStoreFile in user Gradle properties")
            storeFile = file(storeFilePath)
            storePassword = signingProperty("reelCounterReleaseStorePassword")
                ?: error("Missing reelCounterReleaseStorePassword in user Gradle properties")
            keyAlias = signingProperty("reelCounterReleaseKeyAlias")
                ?: error("Missing reelCounterReleaseKeyAlias in user Gradle properties")
            keyPassword = signingProperty("reelCounterReleaseKeyPassword")
                ?: error("Missing reelCounterReleaseKeyPassword in user Gradle properties")
        }
    }

    defaultConfig {
        applicationId = "com.reelcounter.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.core:core-ktx:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
