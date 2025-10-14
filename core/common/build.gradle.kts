import com.teammanduk.adego.extentions.setNamespace

plugins {
    alias(libs.plugins.adego.android.library)
}

setNamespace("core.common")

dependencies {
    implementation(libs.androidx.core.ktx)
}
