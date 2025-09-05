package com.teammanduk.adego.extentions

import org.gradle.api.Project


fun Project.setNamespace(name: String) {
    androidExtension.apply {
        namespace = "com.teammanduk.adego.$name"
    }
}