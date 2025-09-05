plugins {
    alias(libs.plugins.adego.android.application)
    alias(libs.plugins.adego.android.compose)
}

android {
    namespace = "com.teammanduk.adego"

    defaultConfig {
        applicationId = "com.teammanduk.adego"
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.feature.main)
}