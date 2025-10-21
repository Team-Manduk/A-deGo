import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
}

setNamespace("sync.location")

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.notifications)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
