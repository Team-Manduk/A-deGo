import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.data")

dependencies {
    implementation(projects.core.dataApi)

    // Google Places API
    implementation(libs.play.services.places)

    // Network - Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
}