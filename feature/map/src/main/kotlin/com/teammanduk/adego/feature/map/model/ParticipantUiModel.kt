package com.teammanduk.adego.feature.map.model

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.feature.map.util.EtaFormatter
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class ParticipantUiModel(
    val userId: String,
    val name: String,
    val color: Color,
    val status: String,
    val eta: String? = null,
    val distance: String? = null,
    val location: ParticipantLocation? = null,
    val route: ParticipantRoute? = null,
    val distanceToDestination: Int? = null
)

/**
 * Participant를 UI 모델로 변환
 * @param destination 목적지 정보 (거리 계산용, null이면 거리 미계산)
 */
fun Participant.toUiModel(destination: Place? = null): ParticipantUiModel {
    // Domain Model에서 원본 숫자 데이터를 가져와 UI에서 포맷팅
    val formattedEta = route?.durationInSeconds?.let { EtaFormatter.formatTime(it) }
    val formattedDistance = route?.distanceInMeters?.let { EtaFormatter.formatDistance(it) }

    // 목적지와 현재 위치가 모두 있으면 직선거리 계산
    val distanceToDestination = if (destination != null && location != null) {
        calculateDistance(
            lat1 = location.latitude,
            lon1 = location.longitude,
            lat2 = destination.latitude,
            lon2 = destination.longitude
        )
    } else {
        null
    }

    return ParticipantUiModel(
        userId = this.userId,
        name = this.name,
        color = this.getParticipantColor(),
        status = if (formattedEta != null) "도착중" else "대기중",
        eta = formattedEta,
        distance = formattedDistance,
        location = this.location,
        route = this.route,
        distanceToDestination = distanceToDestination
    )
}

/**
 * 두 좌표 간 직선거리 계산 (Haversine formula)
 * @return 거리 (미터)
 */
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val earthRadiusMeters = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return (earthRadiusMeters * c).toInt()
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

/**
 * Participant 리스트를 UI 모델 리스트로 변환
 * @param destination 목적지 정보 (거리 계산용, null이면 거리 미계산)
 */
fun List<Participant>.toUiModels(destination: Place? = null): List<ParticipantUiModel> {
    return map { it.toUiModel(destination) }
}
