import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.feature)
}

setNamespace("feature.create")

dependencies {
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
}
