import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.feature)
}

setNamespace("feature.main")

dependencies {
    implementation(projects.feature.home)
    implementation(projects.feature.create)
    implementation(projects.feature.map)
}