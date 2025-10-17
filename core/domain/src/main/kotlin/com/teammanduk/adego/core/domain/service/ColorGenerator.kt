package com.teammanduk.adego.core.domain.service

import javax.inject.Inject
import kotlin.math.abs

/**
 * 사용자 ID 기반 고유 색상을 생성하는 도메인 서비스
 *
 * 책임:
 * - userId를 기반으로 결정론적 색상 생성 (같은 ID는 항상 같은 색상)
 * - 시각적으로 구분 가능한 색상 생성
 *
 * 특징:
 * - Android Framework 의존성 없음 (순수 Kotlin)
 * - HSV 색상 모델 사용
 * - 단위 테스트 가능
 */
class ColorGenerator @Inject constructor() {

    /**
     * userId를 기반으로 고유한 색상을 생성
     *
     * @param userId 사용자 고유 ID
     * @return HEX 색상 코드 (예: "#FF5733")
     */
    fun generateColorFromUserId(userId: String): String {
        // userId의 해시코드 생성
        val hash = userId.hashCode()

        // HSV 색상 공간 사용 (Hue, Saturation, Value)
        // Hue: 0-360 범위로 매핑하여 다양한 색상 생성
        val hue = abs(hash % 360)

        // 채도와 명도를 고정하여 선명하고 보기 좋은 색상 생성
        val saturation = 0.7f  // 70% 채도
        val value = 0.9f       // 90% 명도

        // HSV를 RGB로 변환
        val rgb = hsvToRgb(hue.toFloat(), saturation, value)

        // #RRGGBB 형식으로 반환
        return String.format("#%06X", 0xFFFFFF and rgb)
    }

    /**
     * HSV 색상 값을 RGB 정수 값으로 변환
     *
     * @param hue 색상 (0-360)
     * @param saturation 채도 (0.0-1.0)
     * @param value 명도 (0.0-1.0)
     * @return RGB 정수 값
     */
    private fun hsvToRgb(hue: Float, saturation: Float, value: Float): Int {
        val h = hue / 60f
        val c = value * saturation
        val x = c * (1 - abs((h % 2) - 1))
        val m = value - c

        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val red = ((r + m) * 255).toInt()
        val green = ((g + m) * 255).toInt()
        val blue = ((b + m) * 255).toInt()

        return (red shl 16) or (green shl 8) or blue
    }
}
