import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.remote")

dependencies {
    implementation(projects.core.dataApi)
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.auth.ktx)

    // Location Services
    implementation(libs.play.services.location)
}