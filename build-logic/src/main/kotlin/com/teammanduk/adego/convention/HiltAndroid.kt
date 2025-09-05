package com.teammanduk.adego.convention

import com.teammanduk.adego.extentions.implementation
import com.teammanduk.adego.extentions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureHiltAndroid() {
    with(pluginManager) {
        apply("com.google.dagger.hilt.android")
        apply("com.google.devtools.ksp")
    }

    dependencies {
        implementation(libs.findLibrary("hilt-android").get())
        "ksp"(libs.findLibrary("hilt-android-compiler").get())
        "kspAndroidTest"(libs.findLibrary("hilt-android-compiler").get())
    }
}

internal class HiltAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureHiltAndroid()
    }
}