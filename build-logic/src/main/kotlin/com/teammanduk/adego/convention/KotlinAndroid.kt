package com.teammanduk.adego.convention

import com.teammanduk.adego.extentions.androidExtension
import com.teammanduk.adego.extentions.coreLibraryDesugaring
import com.teammanduk.adego.extentions.implementation
import com.teammanduk.adego.extentions.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinAndroid() {
    pluginManager.apply("org.jetbrains.kotlin.android")

    androidExtension.apply {
        compileSdk = 36

        defaultConfig {
            minSdk = 26
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(17)
        }

        configureKotlinCommon()

        dependencies {
            implementation(project(":core:domain"))

            implementation(libs.findLibrary("kotlin-standard").get())
            coreLibraryDesugaring(libs.findLibrary("android-desugarJdkLibs").get())
        }
    }

}

internal fun Project.configureKotlinCommon() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)

            val warningsAsErrors: String? by project

            allWarningsAsErrors.set(warningsAsErrors.toBoolean())
            freeCompilerArgs.set(
                freeCompilerArgs.get() + listOf("-opt-in=kotlin.RequiresOptIn")
            )
        }
    }
}

internal fun Project.configureKotlinJvmLibrary() {
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    configureKotlinCommon()
}