import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
}

setNamespace("core.local")

dependencies {
    implementation(projects.core.dataApi)
    implementation(projects.core.model)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
}
