import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.remote")

dependencies {
    implementation(projects.core.dataApi)
    implementation(libs.kotlinx.serialization.json)

    // Network - Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth.ktx)

    // Location Services
    implementation(libs.play.services.location)
}