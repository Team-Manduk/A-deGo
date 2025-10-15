import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.feature)
}

setNamespace("feature.main")

dependencies {
    implementation(projects.feature.home)
    implementation(projects.feature.create)
    implementation(projects.feature.map)
    implementation(projects.feature.place)
    implementation(projects.feature.route)
    implementation(projects.core.data)
    implementation(projects.core.remote)
}