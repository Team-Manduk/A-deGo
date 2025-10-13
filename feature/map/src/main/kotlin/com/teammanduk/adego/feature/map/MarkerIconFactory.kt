package com.teammanduk.adego.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * 참가자 마커 아이콘을 생성하는 팩토리 함수
 * Material Icons Person의 정확한 path를 사용하여 트래커/카드와 동일한 아이콘 생성
 *
 * @param color 참가자 색상
 * @param isSelected 선택 여부 (선택 시 크기가 커짐)
 * @return Google Maps Marker에 사용할 BitmapDescriptor
 */
@Composable
fun createParticipantMarkerIcon(
    color: Color,
    isSelected: Boolean = false
): BitmapDescriptor {
    val density = androidx.compose.ui.platform.LocalDensity.current

    val sizeDp = if (isSelected) 56 else 48 // 크기
    val sizePx = with(density) { sizeDp.dp.roundToPx() }

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val outerRadius = sizePx / 2f - 6f // 여백
    val innerRadius = outerRadius - 6f // 테두리 두께

    // 흰색 테두리
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, outerRadius, paint)

    // 참가자 색상 원
    paint.color = color.toArgb()
    canvas.drawCircle(centerX, centerY, innerRadius, paint)

    // Person 아이콘 (Material Icons의 정확한 path)
    paint.color = android.graphics.Color.WHITE

    // 아이콘 크기 계산 (트래커 바와 동일하게)
    val iconSize = innerRadius * 1.2f
    val scale = iconSize / 24f // Material Icons는 24x24 viewBox
    val iconOffsetX = centerX - (iconSize / 2f)
    val iconOffsetY = centerY - (iconSize / 2f)

    // Material Icons Person path: M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z

    // 머리 (원): center(12,8), radius 4
    canvas.drawCircle(
        12f * scale + iconOffsetX,
        8f * scale + iconOffsetY,
        4f * scale,
        paint
    )

    // 몸통: 하단 사각형 영역
    val bodyPath = android.graphics.Path().apply {
        // 사각형 윤곽
        moveTo(4f * scale + iconOffsetX, 18f * scale + iconOffsetY)
        lineTo(4f * scale + iconOffsetX, 20f * scale + iconOffsetY)
        lineTo(20f * scale + iconOffsetX, 20f * scale + iconOffsetY)
        lineTo(20f * scale + iconOffsetX, 18f * scale + iconOffsetY)

        // 상단 곡선 (c-2.67 0-8 1.34-8 4)
        cubicTo(
            20f * scale + iconOffsetX, 15.34f * scale + iconOffsetY,
            14.67f * scale + iconOffsetX, 14f * scale + iconOffsetY,
            12f * scale + iconOffsetX, 14f * scale + iconOffsetY
        )
        cubicTo(
            9.33f * scale + iconOffsetX, 14f * scale + iconOffsetY,
            4f * scale + iconOffsetX, 15.34f * scale + iconOffsetY,
            4f * scale + iconOffsetX, 18f * scale + iconOffsetY
        )
        close()
    }
    canvas.drawPath(bodyPath, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
