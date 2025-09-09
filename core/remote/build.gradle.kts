import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.remote")

dependencies {
    implementation(projects.core.dataApi)
    implementation(libs.kotlinx.serialization.json)
}