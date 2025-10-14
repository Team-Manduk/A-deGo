package com.teammanduk.adego.feature.map.model

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import kotlin.math.absoluteValue

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

fun Participant.toUiModel(): ParticipantUiModel = ParticipantUiModel(
    userId = this.userId,
    name = this.name,
    color = this.getParticipantColor(),
    status = if (route?.eta != null) "도착중" else "대기중",
    eta = this.route?.eta,
    distance = this.route?.distance,
    location = this.location,
    route = this.route,
    distanceToDestination = this.distanceToDestination
)

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
