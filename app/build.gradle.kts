plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zaid.hologramazaid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zaid.hologramazaid"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.4.1"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.termux.termux-app:terminal-view:0.118.0")
}
