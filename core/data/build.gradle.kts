import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
}

setNamespace("core.data")

dependencies {
    implementation(projects.core.dataApi)
}