plugins {
    alias(libs.plugins.adego.kotlin.library)
}

dependencies {
    // Model을 api로 노출 (UseCase가 Place 반환하므로)
    api(projects.core.model)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Hilt (javax.inject 포함)
    implementation(libs.hilt.core)
}