import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.feature)
}

setNamespace("feature.route")

dependencies {
    implementation(projects.core.data)
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material.icons.core)

}
