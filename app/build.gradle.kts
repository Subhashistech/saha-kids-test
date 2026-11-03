plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Each GitHub build gets a higher version number, so a new APK installs over the old one.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.saha.kidstest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.saha.kidstest"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // One fixed signing key, so updates keep the children's saved scores and questions.
    signingConfigs {
        create("saha") {
            storeFile = file("saha.keystore")
            storePassword = "sahakids"
            keyAlias = "saha"
            keyPassword = "sahakids"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("saha")
        }
        debug {
            signingConfig = signingConfigs.getByName("saha")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
