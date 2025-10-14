import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.feature)
}

setNamespace("feature.place")

dependencies {
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
}
