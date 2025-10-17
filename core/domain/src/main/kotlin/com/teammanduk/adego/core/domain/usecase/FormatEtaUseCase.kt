package com.teammanduk.adego.core.domain.usecase

import javax.inject.Inject

/**
 * ETA 시간과 거리를 사용자가 읽기 쉬운 형식으로 포맷팅하는 UseCase
 */
class FormatEtaUseCase @Inject constructor() {

    data class FormattedEta(
        val timeString: String,
        val distanceString: String
    )

    operator fun invoke(
        remainingTimeInSeconds: Int,
        remainingDistanceInMeters: Double
    ): FormattedEta {
        return FormattedEta(
            timeString = formatTime(remainingTimeInSeconds),
            distanceString = formatDistance(remainingDistanceInMeters)
        )
    }

    /**
     * 남은 시간을 문자열로 포맷 (예: "15분", "1시간 30분")
     */
    private fun formatTime(seconds: Int): String {
        val minutes = (seconds / 60).coerceAtLeast(1)
        return when {
            minutes < 60 -> "${minutes}분"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0) {
                    "${hours}시간"
                } else {
                    "${hours}시간 ${remainingMinutes}분"
                }
            }
        }
    }

    /**
     * 남은 거리를 문자열로 포맷 (예: "3.2km", "850m")
     */
    private fun formatDistance(meters: Double): String {
        return when {
            meters >= 1000 -> String.format("%.1fkm", meters / 1000)
            else -> String.format("%.0fm", meters)
        }
    }
}
