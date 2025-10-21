import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.ksp)
}

setNamespace("core.local")

dependencies {
    implementation(projects.core.dataApi)
    implementation(projects.core.model)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlinx Serialization (TMAP API와 동일한 직렬화)
    implementation(libs.kotlinx.serialization.json)
}
