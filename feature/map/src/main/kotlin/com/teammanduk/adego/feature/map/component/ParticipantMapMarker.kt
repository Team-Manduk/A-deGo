package com.teammanduk.adego.feature.map.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.teammanduk.adego.feature.map.model.ParticipantUiModel

/**
 * 화면 밖 참가자 위치 마커 (오버레이)
 */
@Composable
internal fun ParticipantMapMarker(
    participant: ParticipantUiModel,
    isSelected: Boolean,
    cameraPositionState: CameraPositionState
) {
    participant.location?.let { location ->
        // 카메라 position이 변경될 때마다 리컴포지션
        val cameraPosition = cameraPositionState.position

        // projection 계산을 derivedStateOf로 최적화
        val screenPosition by derivedStateOf {
            cameraPositionState.projection?.toScreenLocation(
                LatLng(location.latitude, location.longitude)
            )
        }

        // 화면 크기 가져오기
        val configuration = LocalWindowInfo.current.containerSize
        val screenWidthPx = with(LocalDensity.current) {
            configuration.width.dp.toPx()
        }
        val screenHeightPx = with(LocalDensity.current) {
            configuration.height.dp.toPx()
        }

        // 화면 안/밖 판단
        val isOnScreen = screenPosition?.let { pos ->
            pos.x >= 0f && pos.x <= screenWidthPx && pos.y >= 0f && pos.y <= screenHeightPx
        } ?: false

        // 화면 밖인 경우만 오버레이로 표시 (나중에 화살표로 변경 예정)
        if (!isOnScreen) {
            screenPosition?.let { screenPos ->
                // px를 dp로 변환
                val density = LocalDensity.current
                val offsetX = with(density) { (screenPos.x - 28f).toDp() }
                val offsetY = with(density) { (screenPos.y - 28f).toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 심장 박동 동심원 애니메이션 (선택된 경우만)
                    if (isSelected) {
                        val infiniteTransition =
                            rememberInfiniteTransition(label = "mapPulse_${participant.userId}")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000)
                            ),
                            label = "mapPulseScale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.6f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000)
                            ),
                            label = "mapPulseAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(40.dp * pulseScale)
                                .clip(CircleShape)
                                .background(participant.color.copy(alpha = pulseAlpha))
                        )
                    }

                    // 메인 마커
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(participant.color)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = participant.name,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
