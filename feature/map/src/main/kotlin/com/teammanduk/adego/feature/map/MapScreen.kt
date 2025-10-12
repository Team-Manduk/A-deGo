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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
    roomId: String,
    meetingTime: String = "00시 00분",
    viewModel: MapViewModel,
) {
    val room by viewModel.room.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val selectedParticipantIndex by viewModel.selectedParticipantIndex.collectAsState()
    val isInitialLocationLoaded by viewModel.isInitialLocationLoaded.collectAsState()

    // 위치 권한 요청
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            android.util.Log.d("A-degoLogTag", "[MapScreen] 위치 권한 승인됨 - 위치 추적 시작")
            viewModel.startLocationTracking()
        } else {
            android.util.Log.w("A-degoLogTag", "[MapScreen] 위치 권한 거부됨")
        }
    }

    // 화면 진입 시 권한 요청
    LaunchedEffect(Unit) {
        android.util.Log.d("A-degoLogTag", "[MapScreen] 위치 권한 요청 시작")
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // 초기 위치를 로드할 때까지 로딩 화면 표시
    if (!isInitialLocationLoaded) {
        LoadingScreen()
    } else {
        MapScreen(
            roomId = roomId,
            meetingTime = meetingTime,
            participants = participants,
            selectedParticipantIndex = selectedParticipantIndex,
            onParticipantSelected = viewModel::onParticipantSelected,
            destination = room?.destination,
            currentUserId = viewModel.userId
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AdegoTheme.colors.main500,
                strokeWidth = 4.dp
            )
            Text(
                text = "현재 위치를 가져오는 중...",
                style = AdegoTheme.typography.bodyLarge,
                color = AdegoTheme.colors.onBackground
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapScreen(
    roomId: String,
    meetingTime: String,
    participants: List<com.teammanduk.adego.core.model.Participant>,
    selectedParticipantIndex: Int,
    onParticipantSelected: (Int) -> Unit,
    destination: com.teammanduk.adego.core.model.Place?,
    currentUserId: String
) {
    // 임시: UI 전용 Participant 데이터로 변환
    val uiParticipants = remember(participants) {
        participants.mapIndexed { index, participant ->
            // profileColor가 비어있거나 유효하지 않으면 기본 색상 사용
            val parsedColor = try {
                if (participant.profileColor.isNotEmpty()) {
                    Color(android.graphics.Color.parseColor(participant.profileColor))
                } else {
                    Color(0xFFE53935) // 기본 빨강
                }
            } catch (e: Exception) {
                android.util.Log.w("A-degoLogTag", "[MapScreen] profileColor 파싱 실패: ${participant.profileColor}, 기본 색상 사용")
                Color(0xFFE53935) // 기본 빨강
            }

            Participant(
                name = participant.name,
                distanceInMeters = participant.route?.distanceInMeters ?: 0,
                status = if (participant.route?.eta != null) "도착중" else "대기중",
                color = parsedColor
            )
        }
    }

    val pagerState = rememberPagerState(
        pageCount = { uiParticipants.size },
        initialPage = selectedParticipantIndex
    )
    val coroutineScope = rememberCoroutineScope()
    var showInviteDialog by remember { mutableStateOf(false) }

    // 현재 사용자 찾기
    val currentUser = remember(participants, currentUserId) {
        participants.find { it.userId == currentUserId }
    }

    // 초기 렌더링 여부 추적
    var isInitialRender by remember { mutableStateOf(true) }

    // 지도 카메라 위치 설정 - 현재 사용자 위치가 반드시 있어야 함
    val cameraPositionState = rememberCameraPositionState {
        currentUser?.location?.let { location ->
            android.util.Log.d("A-degoLogTag", "[MapScreen] 카메라 초기화: 현재 사용자 위치 lat=${location.latitude}, lng=${location.longitude}")
            position = CameraPosition.fromLatLngZoom(
                LatLng(location.latitude, location.longitude),
                15f
            )
        } ?: run {
            // 이 경우는 발생하면 안 됨 (로딩 화면에서 위치를 받은 후에만 여기 도달)
            android.util.Log.w("A-degoLogTag", "[MapScreen] 경고: 현재 사용자 위치가 없음")
            position = CameraPosition.fromLatLngZoom(LatLng(37.5665, 126.9780), 15f)
        }
    }

    // 현재 사용자의 위치가 업데이트되면 카메라 이동 (초기 렌더링은 제외)
    LaunchedEffect(currentUser?.location) {
        currentUser?.location?.let { location ->
            if (isInitialRender) {
                // 초기 렌더링: 애니메이션 없이 바로 이동
                android.util.Log.d("A-degoLogTag", "[MapScreen] 초기 카메라 위치 설정: lat=${location.latitude}, lng=${location.longitude}")
                cameraPositionState.move(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f
                    )
                )
                isInitialRender = false
            } else {
                // 이후 업데이트: 부드럽게 애니메이션
                android.util.Log.d("A-degoLogTag", "[MapScreen] 현재 사용자 위치 업데이트: lat=${location.latitude}, lng=${location.longitude}")
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f
                    ),
                    durationMs = 1000
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // 전체 화면 지도
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // 목적지 마커 추가
            destination?.let {
                com.google.maps.android.compose.Marker(
                    state = com.google.maps.android.compose.rememberMarkerState(
                        position = LatLng(it.latitude, it.longitude)
                    ),
                    title = it.name,
                    snippet = "목적지"
                )
            }
        }

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
                        text = "모임인원: ${uiParticipants.size}명",
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
                uiParticipants.forEachIndexed { index, participant ->
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

        // 지도 위 참가자 위치 마커들
        participants.forEach { participant ->
            participant.location?.let { location ->
                // 위도/경도를 화면 좌표로 변환
                val projection = cameraPositionState.projection
                val screenPosition = projection?.toScreenLocation(
                    LatLng(location.latitude, location.longitude)
                )

                screenPosition?.let { screenPos ->
                    val isSelected = uiParticipants.getOrNull(pagerState.currentPage)?.name == participant.name

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
                            val infiniteTransition = rememberInfiniteTransition(label = "mapPulse_${participant.userId}")
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
                    participant = uiParticipants[page],
                    currentPage = pagerState.currentPage,
                    totalPages = uiParticipants.size,
                    onPreviousClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    onNextClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < uiParticipants.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    }

    // 초대 다이얼로그
    if (showInviteDialog) {
        InviteDialog(
            inviteCode = roomId,
            onDismiss = { showInviteDialog = false }
        )
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
            roomId = "preview123",
            meetingTime = "14시 30분",
            participants = emptyList(),
            selectedParticipantIndex = 0,
            onParticipantSelected = {},
            destination = null,
            currentUserId = "preview_user"
        )
    }
}
