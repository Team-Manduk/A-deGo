package com.teammanduk.adego.feature.map

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.model.LatLng
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme

// UI 전용 참가자 데이터 클래스
internal data class Participant(
    val name: String,
    val distanceInMeters: Int,
    val status: String,
    val color: Color
)

/**
 * 참가자 트래킹바 컴포넌트
 * 목적지를 기준으로 참가자들의 거리를 시각화
 * 펄스 애니메이션이 바 영역을 넘어설 수 있도록 레이어 분리
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ParticipantTrackingBar(
    participants: List<com.teammanduk.adego.core.model.Participant>,
    uiParticipants: List<Participant>,
    destination: com.teammanduk.adego.core.model.Place?,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onParticipantClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 외부 Box: 클리핑 없이 펄스 효과가 넘칠 수 있도록
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // 펄스가 넘칠 공간 확보
    ) {
        // 배경 바 (클리핑됨)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
                .background(
                    color = Color(0xFFD6E9F5),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 12.dp)
        ) {
            val barWidth = maxWidth - 24.dp // padding 제외한 실제 트래킹바 너비

            // 약속장소 아이콘 (왼쪽 끝, 0%)
            destination?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AdegoTheme.colors.main500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "약속장소",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 참가자 아이콘들 (펄스 효과 포함, 클리핑되지 않도록 별도 레이어)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // 전체 높이 사용
                .align(Alignment.Center)
                .padding(horizontal = 12.dp)
        ) {
            val barWidth = maxWidth - 24.dp

            // 거리 정보가 있는 참가자들로 필터링
            val participantsWithDistance =
                participants.filter { it.distanceToDestination != null }
            val currentMaxDistance =
                participantsWithDistance.maxOfOrNull { it.distanceToDestination!! } ?: 1000

            // 최대 거리 기록
            var maxRecordedDistance by remember { mutableStateOf(1000) }
            if (currentMaxDistance > maxRecordedDistance) {
                maxRecordedDistance = currentMaxDistance
            }

            val trackerMaxDistance = maxRecordedDistance.coerceAtLeast(1000)

            participantsWithDistance.forEach { participant ->
                val uiParticipant = uiParticipants.find { it.name == participant.name }

                uiParticipant?.let { uiPart ->
                    val index = uiParticipants.indexOf(uiPart)
                    val isSelected = pagerState.currentPage == index

                    val iconSize by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 28.dp,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 200f
                        ),
                        label = "iconSize"
                    )

                    // normalizedPosition: 실제 거리를 트래커 최대 거리로 나눔
                    val normalizedPosition = if (trackerMaxDistance > 0) {
                        (participant.distanceToDestination!!.toFloat() / trackerMaxDistance.toFloat()).coerceIn(
                            0f,
                            1f
                        )
                    } else {
                        0f
                    }

                    // 실제 offset 계산
                    val offsetX = 36.dp + (barWidth - 72.dp) * normalizedPosition

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = 0.dp)
                            .size(56.dp) // 펄스가 확장될 공간
                            .align(Alignment.CenterStart)
                            .zIndex(if (isSelected) 1f else 0f), // 선택된 아이콘을 최상단에 표시
                        contentAlignment = Alignment.Center
                    ) {
                        // 펄스 애니메이션 (선택된 경우만, 뒤에 배치)
                        if (isSelected) {
                            val infiniteTransition =
                                rememberInfiniteTransition(label = "pulse_${uiPart.name}")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 2.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000)
                                ),
                                label = "pulseScale"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000)
                                ),
                                label = "pulseAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(iconSize * pulseScale)
                                    .background(
                                        color = uiPart.color.copy(alpha = pulseAlpha),
                                        shape = CircleShape
                                    )
                            )
                        }

                        // 메인 아이콘 (앞에 배치)
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color.White, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(uiPart.color)
                                .clickable { onParticipantClick(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = uiPart.name,
                                tint = Color.White,
                                modifier = Modifier.size(if (isSelected) 18.dp else 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 화면 밖에 있는 참가자를 표시하는 마커 컴포넌트
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ParticipantMapMarker(
    participant: com.teammanduk.adego.core.model.Participant,
    uiParticipants: List<Participant>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState
) {
    participant.location?.let { location ->
        // 카메라 position이 변경될 때마다 리컴포지션
        val cameraPosition = cameraPositionState.position

        // projection 계산을 derivedStateOf로 최적화
        val screenPosition by androidx.compose.runtime.derivedStateOf {
            cameraPositionState.projection?.toScreenLocation(
                LatLng(location.latitude, location.longitude)
            )
        }

        // 화면 크기 가져오기
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            configuration.screenWidthDp.dp.toPx()
        }
        val screenHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            configuration.screenWidthDp.dp.toPx()
        }

        // 화면 안/밖 판단
        val isOnScreen = screenPosition?.let { pos ->
            pos.x >= 0f && pos.x <= screenWidthPx && pos.y >= 0f && pos.y <= screenHeightPx
        } ?: false

        // 화면 밖인 경우만 오버레이로 표시 (나중에 화살표로 변경 예정)
        if (!isOnScreen) {
            screenPosition?.let { screenPos ->
                val isSelected =
                    uiParticipants.getOrNull(pagerState.currentPage)?.name == participant.name

                // 참가자 색상 찾기
                val participantColor = uiParticipants.find { it.name == participant.name }?.color
                    ?: Color(0xFFE53935)

                // px를 dp로 변환
                val density = androidx.compose.ui.platform.LocalDensity.current
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
                                .background(participantColor.copy(alpha = pulseAlpha))
                        )
                    }

                    // 메인 마커
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(participantColor)
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

/**
 * 참가자 카드 컴포넌트
 */
@Composable
internal fun ParticipantCard(
    participant: Participant,
    currentPage: Int,
    totalPages: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 프로필 + 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 아이콘
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(participant.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            // 이름 + 상태
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = participant.name,
                    style = AdegoTheme.typography.headlineLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Text(
                    text = participant.status,
                    style = AdegoTheme.typography.bodyLarge,
                    color = Color(0xFF9E9E9E)
                )
            }
        }

        // 화살표 + 인디케이터
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이전 버튼
            IconButton(
                onClick = onPreviousClick,
                enabled = currentPage > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전",
                    tint = if (currentPage > 0) AdegoTheme.colors.main500 else Color(0xFFE0E0E0),
                    modifier = Modifier.size(32.dp)
                )
            }

            // 페이지 인디케이터
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage) AdegoTheme.colors.main500
                                else Color(0xFFE0E0E0)
                            )
                    )
                }
            }

            // 다음 버튼
            IconButton(
                onClick = onNextClick,
                enabled = currentPage < totalPages - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음",
                    tint = if (currentPage < totalPages - 1) AdegoTheme.colors.main500 else Color(
                        0xFFE0E0E0
                    ),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
