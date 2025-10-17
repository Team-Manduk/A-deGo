package com.teammanduk.adego.feature.map.util

/**
 * ETA (도착 예정 시간) 포맷팅 유틸리티
 *
 * Domain 모델의 원본 숫자 데이터를 UI에 표시할 문자열로 변환합니다.
 */
object EtaFormatter {

    /**
     * 남은 시간을 문자열로 포맷 (예: "15분", "1시간 30분")
     *
     * @param seconds 남은 시간 (초)
     * @return 포맷된 시간 문자열
     */
    fun formatTime(seconds: Int): String {
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
     *
     * @param meters 남은 거리 (미터)
     * @return 포맷된 거리 문자열
     */
    fun formatDistance(meters: Int): String {
        return when {
            meters >= 1000 -> String.format("%.1fkm", meters / 1000.0)
            else -> "${meters}m"
        }
    }
}
