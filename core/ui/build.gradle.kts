import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
    alias(libs.plugins.adego.android.compose)
}

setNamespace("core.ui")

dependencies {
    implementation(project(":core:designsystem"))
}
