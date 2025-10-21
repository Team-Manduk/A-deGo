import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.dataApi")

dependencies {
    implementation(libs.kotlinx.serialization.json)
}