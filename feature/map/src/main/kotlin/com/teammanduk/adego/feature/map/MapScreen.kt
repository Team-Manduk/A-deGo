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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import kotlinx.coroutines.launch
import kotlin.math.atan2

// 참가자 데이터 클래스
private data class Participant(
    val name: String,
    val distanceInMeters: Int,
    val status: String,
    val color: Color
)

@Composable
internal fun MapRoute(
    meetingTime: String = "00시 00분",
) {
    MapScreen(
        meetingTime = meetingTime
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapScreen(
    meetingTime: String
) {
    // 임시 참가자 데이터
    val participants = remember {
        listOf(
            Participant("홍길동", 0, "잠시 후 도착", Color(0xFFE53935)),
            Participant("김철수", 300, "도착중", Color(0xFF5C6BC0)),
            Participant("이영희", 500, "도착중", Color(0xFF43A047)),
            Participant("박민수", 800, "도착중", Color(0xFFD81B60)),
            Participant("최영수", 1200, "도착중", Color(0xFF00ACC1))
        )
    }

    val pagerState = rememberPagerState(pageCount = { participants.size })
    val coroutineScope = rememberCoroutineScope()
    var showInviteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // 전체 화면 지도
        MapPlaceholder(modifier = Modifier.fillMaxSize())

        // 상단 영역
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
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
                        text = "모임인원: ${participants.size}명",
                        style = AdegoTheme.typography.bodyLarge,
                        color = AdegoTheme.colors.highlight500
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AdegoTheme.colors.highlight500)
                            .clickable { showInviteDialog = true },
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

            // 참가자 아이콘 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFD6E9F5),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                participants.forEachIndexed { index, participant ->
                    val isSelected = pagerState.currentPage == index
                    val iconSize by animateDpAsState(
                        targetValue = if (isSelected) 40.dp else 32.dp,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 200f
                        ),
                        label = "iconSize"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 심장 박동 동심원 애니메이션 (선택된 경우만)
                        if (isSelected) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000)
                                ),
                                label = "pulseScale"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000)
                                ),
                                label = "pulseAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(iconSize * pulseScale)
                                    .clip(CircleShape)
                                    .background(participant.color.copy(alpha = pulseAlpha))
                            )
                        }

                        // 메인 아이콘
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .background(participant.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = participant.name,
                                tint = Color.White,
                                modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 하단 참가자 카드 (스와이프)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp),
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

        // 지도 위 참가자 위치 마커들 (상단/하단 패딩으로 영역 제한)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 200.dp, bottom = 240.dp)  // 상단 섹션과 하단 카드 영역 제외
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            participants.forEachIndexed { index, participant ->
                // TODO: 실제 위치 데이터로 대체 필요
                // 임시로 위치 표시 (일부는 화면 밖, 다양한 방향으로 분산)
                val offsetX = when (participant.name) {
                    "홍길동" -> screenWidth / 2  // 화면 내
                    "김철수" -> screenWidth + 100.dp  // 화면 밖 (오른쪽)
                    "이영희" -> screenWidth / 2  // 화면 밖 (아래)
                    "박민수" -> -100.dp  // 화면 밖 (왼쪽)
                    "최영수" -> screenWidth / 2  // 화면 밖 (위)
                    else -> 150.dp
                }
                val offsetY = when (participant.name) {
                    "홍길동" -> screenHeight / 2  // 화면 내
                    "김철수" -> screenHeight / 2  // 화면 밖 (오른쪽)
                    "이영희" -> screenHeight + 100.dp  // 화면 밖 (아래)
                    "박민수" -> screenHeight / 2  // 화면 밖 (왼쪽)
                    "최영수" -> -50.dp  // 화면 밖 (위)
                    else -> screenHeight / 2
                }

                // 경계값 정의 (dp) - 이제 padding된 영역 내부 기준
                val topBound = 0.dp
                val bottomBound = screenHeight
                val leftBound = 16.dp
                val rightBound = screenWidth - 80.dp

                val isSelected = pagerState.currentPage == index
                val isOutOfBounds = offsetX < leftBound || offsetX > rightBound ||
                        offsetY < topBound || offsetY > bottomBound

                if (isOutOfBounds) {
                    // 화면 밖 참가자 - 말풍선으로 표시
                    // 실제 위치를 경계 내로 제한 (말풍선을 표시할 위치)
                    val edgeX = offsetX.coerceIn(leftBound, rightBound)
                    val edgeY = offsetY.coerceIn(topBound, bottomBound)

                    // 말풍선 위치에서 실제 위치로의 방향 (꼬리 방향)
                    val dx = (offsetX - edgeX).value
                    val dy = (offsetY - edgeY).value
                    val angle = atan2(dy, dx)

                    Box(
                        modifier = Modifier
                            .padding(start = edgeX, top = edgeY)
                            .size(48.dp)
                            .align(Alignment.TopStart)
                            .drawBehind {
                                // 원형 배경
                                drawCircle(
                                    color = participant.color,
                                    radius = 20.dp.toPx(),
                                    center = center
                                )

                                // 말풍선 꼬리 (삼각형)
                                val tailSize = 10.dp.toPx()
                                val circleRadius = 20.dp.toPx()

                                // 각도에 따른 꼬리 시작점 계산
                                val tailBaseX = center.x + kotlin.math.cos(angle) * circleRadius
                                val tailBaseY = center.y + kotlin.math.sin(angle) * circleRadius
                                val tailTipX = tailBaseX + kotlin.math.cos(angle) * tailSize
                                val tailTipY = tailBaseY + kotlin.math.sin(angle) * tailSize

                                // 삼각형 양 옆 점 계산
                                val perpAngle1 = angle + Math.PI / 2
                                val perpAngle2 = angle - Math.PI / 2
                                val baseOffset = 6.dp.toPx()

                                val point1X = tailBaseX + kotlin.math.cos(perpAngle1) * baseOffset
                                val point1Y = tailBaseY + kotlin.math.sin(perpAngle1) * baseOffset
                                val point2X = tailBaseX + kotlin.math.cos(perpAngle2) * baseOffset
                                val point2Y = tailBaseY + kotlin.math.sin(perpAngle2) * baseOffset

                                val tailPath = Path().apply {
                                    moveTo(point1X.toFloat(), point1Y.toFloat())
                                    lineTo(tailTipX.toFloat(), tailTipY.toFloat())
                                    lineTo(point2X.toFloat(), point2Y.toFloat())
                                    close()
                                }
                                drawPath(tailPath, participant.color)

                                // 흰색 테두리 - 원
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.White,
                                    radius = 20.dp.toPx(),
                                    center = center,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )

                                // 흰색 테두리 - 꼬리
                                drawPath(
                                    tailPath,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = participant.name,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // 화면 내 참가자 - 일반 마커
                    Box(
                        modifier = Modifier
                            .padding(start = offsetX, top = offsetY)
                            .size(56.dp)
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        // 심장 박동 동심원 애니메이션 (선택된 경우만)
                        if (isSelected) {
                            val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
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
                                .background(participant.color),
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

    // 초대 다이얼로그
    if (showInviteDialog) {
        InviteDialog(
            inviteCode = "ABC123",
            onDismiss = { showInviteDialog = false }
        )
    }
}

@Composable
private fun MapPlaceholder(
    modifier: Modifier = Modifier
) {
    // 서울 강남역 근처 좌표 (기본값)
    val gangnam = LatLng(37.498095, 127.027610)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(gangnam, 15f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        )
    ) {
        // 마커는 필요시 추가
    }
}

@Composable
private fun ParticipantCard(
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

@Composable
private fun InviteDialog(
    inviteCode: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "모임 초대",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onBackground
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = AdegoTheme.colors.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 초대 코드 표시
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "초대 코드",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = AdegoTheme.colors.main500,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inviteCode,
                        style = AdegoTheme.typography.headlineLarge,
                        color = AdegoTheme.colors.main500,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 코드 복사 버튼
            Button(
                onClick = {
                    // TODO: 클립보드에 코드 복사
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "코드 복사",
                    style = AdegoTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SNS 공유 버튼
            OutlinedButton(
                onClick = {
                    // TODO: SNS 공유 기능
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = AdegoTheme.colors.main500
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SNS 공유",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.main500
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MapScreenPreview() {
    AdegoTheme {
        MapScreen(
            meetingTime = "14시 30분"
        )
    }
}
