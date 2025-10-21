package com.teammanduk.adego.core.ui.util

/**
 * 초 단위 시간을 "15분" 또는 "1시간 30분" 형식으로 포맷
 */
fun formatTime(seconds: Int): String {
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
fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format("%.1fkm", meters / 1000.0)
    } else {
        "${meters}m"
    }
}
