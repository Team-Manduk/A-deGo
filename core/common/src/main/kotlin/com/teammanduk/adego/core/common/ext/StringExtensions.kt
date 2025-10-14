package com.teammanduk.adego.core.common.ext

import android.util.Log

/**
 * ISO 8601 형식의 dateTime을 "HH시 mm분" 형식으로 변환
 * 예: "2025-01-13T14:30:00" -> "14시 30분"
 */
fun String.toMeetingTimeFormat(): String {
    return try {
        // ISO 형식 파싱: "2025-01-13T14:30:00" -> "14시 30분"
        val timePart = this.split("T").getOrNull(1) ?: return "00시 00분"
        val timeComponents = timePart.split(":")
        if (timeComponents.size >= 2) {
            val hour = timeComponents[0].toIntOrNull() ?: 0
            val minute = timeComponents[1].toIntOrNull() ?: 0
            String.format("%02d시 %02d분", hour, minute)
        } else {
            "00시 00분"
        }
    } catch (e: Exception) {
        Log.w("StringExtensions", "시간 파싱 실패: $this", e)
        "00시 00분"
    }
}
