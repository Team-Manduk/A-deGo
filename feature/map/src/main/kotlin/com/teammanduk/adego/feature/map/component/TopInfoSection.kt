package com.teammanduk.adego.feature.map.component

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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
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
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.feature.map.model.ParticipantUiModel

/**
 * 지도 상단 정보 영역
 * - 모임 시간 + 인원 + 초대 버튼
 * - 참가자 트래킹바
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TopInfoSection(
    meetingTime: String,
    participantCount: Int,
    participants: List<ParticipantUiModel>,
    destination: Place?,
    pagerState: PagerState,
    onInviteClick: () -> Unit,
    onParticipantClick: (Int) -> Unit,
    onDestinationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 40.dp,
                    bottomEnd = 40.dp
                )
            )
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 시간 + 인원 + 초대 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$meetingTime 모임 시작",
                style = AdegoTheme.typography.titleLarge,
                color = AdegoTheme.colors.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "모임인원: ${participantCount}명",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.highlight500
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AdegoTheme.colors.highlight500)
                        .clickable { onInviteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "초대하기",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 참가자 트래킹 바
        ParticipantTrackingBar(
            participants = participants,
            destination = destination,
            pagerState = pagerState,
            onParticipantClick = onParticipantClick,
            onDestinationClick = onDestinationClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParticipantTrackingBar(
    participants: List<ParticipantUiModel>,
    destination: Place?,
    pagerState: PagerState,
    onParticipantClick: (Int) -> Unit,
    onDestinationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
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
            val barWidth = maxWidth - 24.dp

            destination?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AdegoTheme.colors.main500)
                        .clickable { onDestinationClick() },
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.Center)
                .padding(horizontal = 12.dp)
        ) {
            val barWidth = maxWidth - 24.dp

            val participantsWithDistance =
                participants.filter { it.distanceToDestination != null }
            val currentMaxDistance =
                participantsWithDistance.maxOfOrNull { it.distanceToDestination!! } ?: 1000

            var maxRecordedDistance by remember { mutableStateOf(1000) }

            // 현재 최대 거리가 기록된 거리보다 크면 증가
            if (currentMaxDistance > maxRecordedDistance) {
                maxRecordedDistance = currentMaxDistance
            }
            // 마지막 참가자가 나가서 거리가 줄어들면 현재 거리로 축소
            else if (currentMaxDistance < maxRecordedDistance) {
                maxRecordedDistance = currentMaxDistance
            }

            // 1km 이내에서는 스케일 고정, 그 이상은 동적 스케일
            val trackerMaxDistance = if (maxRecordedDistance <= 1000) {
                1000
            } else {
                maxRecordedDistance
            }

            participantsWithDistance.forEach { participant ->
                val index = participants.indexOf(participant)
                val isSelected = pagerState.currentPage == index

                val iconSize by animateDpAsState(
                    targetValue = if (isSelected) 32.dp else 28.dp,
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 200f
                    ),
                    label = "iconSize"
                )

                val normalizedPosition = if (trackerMaxDistance > 0) {
                    (participant.distanceToDestination!!.toFloat() / trackerMaxDistance.toFloat()).coerceIn(
                        0f,
                        1f
                    )
                } else {
                    0f
                }

                val offsetX = 36.dp + (barWidth - 72.dp) * normalizedPosition

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = 0.dp)
                        .size(56.dp)
                        .align(Alignment.CenterStart)
                        .zIndex(if (isSelected) 1f else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val infiniteTransition =
                            rememberInfiniteTransition(label = "pulse_${participant.name}")
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
                                    color = participant.color.copy(alpha = pulseAlpha),
                                    shape = CircleShape
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color.White, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(participant.color)
                            .clickable { onParticipantClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = participant.name,
                            tint = Color.White,
                            modifier = Modifier.size(if (isSelected) 18.dp else 16.dp)
                        )
                    }
                }
            }
        }
    }
}
