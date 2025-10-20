package com.teammanduk.adego.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.feature.map.model.ParticipantUiModel
import kotlinx.coroutines.launch

@Composable
internal fun ParticipantCardPager(
    participants: List<ParticipantUiModel>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp
        ) { page ->
            ParticipantCard(
                participant = participants[page],
                currentPage = pagerState.currentPage,
                totalPages = participants.size,
                onPreviousClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNextClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < participants.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ParticipantCard(
    participant: ParticipantUiModel,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = participant.name,
                    style = AdegoTheme.typography.headlineLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 이동 상태 표시
                    Text(
                        text = participant.movementStatus,
                        style = AdegoTheme.typography.bodyLarge,
                        color = when (participant.movementStatus) {
                            "도착" -> Color(0xFF4CAF50)
                            "곧 도착" -> Color(0xFFFF9800)
                            "이동 중" -> AdegoTheme.colors.main500
                            "출발 전" -> Color(0xFFFFEB3B).copy(red = 0.8f)
                            else -> Color(0xFF9E9E9E)
                        }
                    )

                    // ETA와 거리 정보가 있으면 추가로 표시
                    if (participant.eta != null && participant.distance != null) {
                        Text(
                            text = "${participant.eta} · ${participant.distance}",
                            style = AdegoTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
