package com.teammanduk.adego.core.ui.permission

import android.Manifest

sealed class PermissionType {
    abstract val permissions: Array<String>
    abstract val rationaleTitle: String
    abstract val rationaleText: String
    abstract val settingsTitle: String
    abstract val settingsText: String

    data object Location : PermissionType() {
        override val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        override val rationaleTitle = "위치 권한 필요"

        override val rationaleText = "실시간 위치 추적 및 모임 장소까지의 거리를 확인하기 위해 위치 권한이 필요합니다."

        override val settingsTitle = "위치 권한 필요"

        override val settingsText = "앱 사용을 위해서는 위치 권한이 필요합니다. 설정에서 직접 권한을 허용해주세요."
    }
}

data class PermissionMessage(
    val title: String,
    val text: String
)

fun List<PermissionType>.toPermissionArray(): Array<String> {
    return this.flatMap { it.permissions.toList() }
        .distinct()
        .toTypedArray()
}

fun List<PermissionType>.toPermissionMessage(
    titleSeparator: String = " 및 ",
    textSeparator: String = "\n\n"
): PermissionMessage {
    val titles = this.map { it.rationaleTitle.replace(" 필요", "") }
    val texts = this.map { it.rationaleText }

    val combinedTitle = titles.joinToString(titleSeparator) + " 필요"
    val combinedText = texts.joinToString(textSeparator)

    return PermissionMessage(combinedTitle, combinedText)
}
