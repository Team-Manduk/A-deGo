import com.teammanduk.adego.extentions.setNamespace
import com.android.build.gradle.LibraryExtension

plugins {
    alias(libs.plugins.adego.android.feature)
}

setNamespace("feature.map")

// BuildConfig 설정
configure<LibraryExtension> {
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG_LOCATION_MODE", "true")
        }
        release {
            buildConfigField("Boolean", "DEBUG_LOCATION_MODE", "false")
        }
    }
}

dependencies {
    implementation(projects.feature.place)
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
}
