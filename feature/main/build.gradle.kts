import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.feature)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}
secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

setNamespace("feature.main")

android {
    buildFeatures {
        buildConfig = true
    }
}
dependencies {
    implementation(projects.feature.home)
    implementation(libs.maps.compose)
}