import java.util.Properties
import java.io.FileInputStream

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

        // Google Maps API Key
        val secretsProperties = Properties()
        val secretsPropertiesFile = rootProject.file("secrets.properties")
        if (secretsPropertiesFile.exists()) {
            secretsProperties.load(FileInputStream(secretsPropertiesFile))
        } else {
            val localDefaultsFile = rootProject.file("local.defaults.properties")
            if (localDefaultsFile.exists()) {
                secretsProperties.load(FileInputStream(localDefaultsFile))
            }
        }
        val mapsApiKey = secretsProperties.getProperty("MAPS_API_KEY", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(projects.feature.main)

    // Google Places API
    implementation(libs.play.services.places)
}