package com.teammanduk.adego.convention

import com.teammanduk.adego.extentions.androidExtension
import com.teammanduk.adego.extentions.debugImplementation
import com.teammanduk.adego.extentions.implementation
import com.teammanduk.adego.extentions.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import kotlin.apply

internal fun Project.configureComposeAndroid() {
    with(plugins) {
        apply("org.jetbrains.kotlin.plugin.compose")
    }

    androidExtension.apply {
        dependencies {
            implementation(platform(libs.findLibrary("androidx-compose-bom").get()))
            implementation(libs.findLibrary("androidx-compose-material3").get())
            implementation(libs.findLibrary("androidx-compose-ui").get())
            implementation(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            implementation(libs.findLibrary("androidx-compose-navigation").get())
            implementation(libs.findLibrary("hilt-navigation-compose").get())

            debugImplementation(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }

    extensions.getByType<ComposeCompilerGradlePluginExtension>().apply {
        includeSourceInformation.set(true)
    }
}