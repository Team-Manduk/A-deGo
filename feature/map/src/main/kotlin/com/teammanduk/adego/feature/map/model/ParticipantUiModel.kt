package com.teammanduk.adego.feature.map.model

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.teammanduk.adego.core.model.MovementStatus
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import kotlin.math.absoluteValue

data class ParticipantUiModel(
    val userId: String,
    val name: String,
    val color: Color,
    val status: String,
    val movementStatus: String,
    val eta: String? = null,
    val distance: String? = null,
    val location: ParticipantLocation? = null,
    val route: ParticipantRoute? = null,
    val distanceToDestination: Int? = null
)

fun Participant.toUiModel(): ParticipantUiModel = ParticipantUiModel(
    userId = this.userId,
    name = this.name,
    color = this.getParticipantColor(),
    status = if (route?.etaInSeconds != null) "도착중" else "대기중",
    movementStatus = this.movementStatus.toKoreanString(),
    eta = this.route?.etaInSeconds?.let { formatTime(it) },
    distance = this.route?.distanceInMeters?.let { formatDistance(it) },
    location = this.location,
    route = this.route,
    distanceToDestination = this.distanceToDestination
)

/**
 * 초 단위 시간을 "15분" 또는 "1시간 30분" 형식으로 포맷
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) {
        "${minutes}분"
    } else {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins > 0) {
            "${hours}시간 ${mins}분"
        } else {
            "${hours}시간"
        }
    }
}

/**
 * 미터 단위 거리를 "3.2km" 또는 "850m" 형식으로 포맷
 */
private fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format("%.1fkm", meters / 1000.0)
    } else {
        "${meters}m"
    }
}

/**
 * MovementStatus를 한국어 문자열로 변환
 */
private fun MovementStatus.toKoreanString(): String {
    return when (this) {
        MovementStatus.NOT_STARTED -> "경로 선택 전"
        MovementStatus.READY -> "출발 전"
        MovementStatus.IN_PROGRESS -> "이동 중"
        MovementStatus.ARRIVING_SOON -> "곧 도착"
        MovementStatus.ARRIVED -> "도착"
    }
}

private fun Participant.getParticipantColor(): Color {
    return if (profileColor.isNotEmpty()) {
        Color(profileColor.toColorInt())
    } else {
        this.toColorFromUserId()
    }
}

private fun Participant.toColorFromUserId(): Color {
    val hue = (this.userId.hashCode().absoluteValue % 360).toFloat()

    return Color.hsv(hue, 0.7f, 0.85f)
}

fun List<Participant>.toUiModels(): List<ParticipantUiModel> {
    return map { it.toUiModel() }
}
