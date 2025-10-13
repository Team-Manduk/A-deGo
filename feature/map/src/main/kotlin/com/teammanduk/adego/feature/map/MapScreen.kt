package com.teammanduk.adego.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
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
    val uiState by viewModel.uiState.collectAsState()

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

    // 현재 사용자 찾기
    val currentUser = uiState.participants.find { it.userId == viewModel.userId }

    // 초기 위치를 로드하고 참가자 정보가 있을 때까지 로딩 화면 표시
    if (!uiState.isInitialLocationLoaded || uiState.participants.isEmpty() || currentUser?.location == null) {
        LoadingScreen()
    } else {
        MapScreen(
            roomId = roomId,
            meetingTime = meetingTime,
            participants = uiState.participants,
            selectedParticipantIndex = uiState.selectedParticipantIndex,
            onAction = viewModel::onAction,
            destination = uiState.room?.destination,
            currentUserId = viewModel.userId,
            initialLocation = currentUser.location!!,
            showInviteDialog = uiState.showInviteDialog
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
    onAction: (MapIntent) -> Unit,
    destination: com.teammanduk.adego.core.model.Place?,
    currentUserId: String,
    initialLocation: com.teammanduk.adego.core.model.ParticipantLocation,
    showInviteDialog: Boolean
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
                android.util.Log.w(
                    "A-degoLogTag",
                    "[MapScreen] profileColor 파싱 실패: ${participant.profileColor}, 기본 색상 사용"
                )
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

    // 현재 사용자 찾기
    val currentUser = remember(participants, currentUserId) {
        participants.find { it.userId == currentUserId }
    }

    // 초기 카메라 설정 완료 여부
    var isCameraInitialized by remember { mutableStateOf(false) }

    // 지도 카메라 위치 설정 (전달받은 초기 위치로 고정)
    val cameraPositionState = rememberCameraPositionState {
        android.util.Log.d(
            "A-degoLogTag",
            "[MapScreen] 카메라 초기 위치 설정: lat=${initialLocation.latitude}, lng=${initialLocation.longitude}"
        )
        position = CameraPosition.fromLatLngZoom(
            LatLng(initialLocation.latitude, initialLocation.longitude),
            15f
        )
    }

    // pagerState 변경을 ViewModel과 동기화 (pagerState -> ViewModel)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedParticipantIndex) {
            onAction(MapIntent.SelectParticipant(pagerState.currentPage))
        }
    }

    // ViewModel 상태를 pagerState와 동기화 (ViewModel -> pagerState)
    LaunchedEffect(selectedParticipantIndex) {
        if (pagerState.currentPage != selectedParticipantIndex) {
            pagerState.animateScrollToPage(selectedParticipantIndex)
        }
    }

    // 참가자 선택 시 해당 위치로 카메라 이동
    LaunchedEffect(selectedParticipantIndex) {
        // 초기화 완료 후에만 카메라 이동 (첫 렌더링 이후)
        if (isCameraInitialized) {
            val selectedParticipant = participants.getOrNull(selectedParticipantIndex)
            selectedParticipant?.location?.let { location ->
                android.util.Log.d(
                    "A-degoLogTag",
                    "[MapScreen] 참가자 선택됨 - ${selectedParticipant.name}, 카메라 이동: lat=${location.latitude}, lng=${location.longitude}"
                )
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f
                    ),
                    durationMs = 500
                )
            }
        } else {
            // 첫 렌더링에서는 초기화만 하고 카메라 이동하지 않음
            isCameraInitialized = true
            android.util.Log.d("A-degoLogTag", "[MapScreen] 카메라 초기화 완료")
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

            // 참가자 마커들 (네이티브 Marker 사용)
            participants.forEach { participant ->
                androidx.compose.runtime.key(participant.userId) {
                    participant.location?.let { location ->
                        val uiParticipant = uiParticipants.find { it.name == participant.name }
                        val isSelected =
                            uiParticipants.getOrNull(pagerState.currentPage)?.name == participant.name

                        uiParticipant?.let { uiPart ->
                            val markerIcon = createParticipantMarkerIcon(uiPart.color, isSelected)

                            com.google.maps.android.compose.Marker(
                                state = com.google.maps.android.compose.rememberMarkerState(
                                    position = LatLng(location.latitude, location.longitude)
                                ),
                                title = participant.name,
                                icon = markerIcon,
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                            )
                        }
                    }
                }
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
                            .clickable { onAction(MapIntent.ShowInviteDialog) },
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

            // 참가자 트래킹 바 (거리 기반 배치)
            ParticipantTrackingBar(
                participants = participants,
                uiParticipants = uiParticipants,
                destination = destination,
                pagerState = pagerState,
                onParticipantClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }

        // 지도 위 참가자 위치 마커들 (화면 밖 처리)
        participants.forEach { participant ->
            androidx.compose.runtime.key(participant.userId) {
                ParticipantMapMarker(
                    participant = participant,
                    uiParticipants = uiParticipants,
                    pagerState = pagerState,
                    cameraPositionState = cameraPositionState
                )
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
            onDismiss = { onAction(MapIntent.DismissInviteDialog) }
        )
    }
}

/**
 * 참가자 트래킹바 컴포넌트
 * 목적지를 기준으로 참가자들의 거리를 시각화
 * 펄스 애니메이션이 바 영역을 넘어설 수 있도록 레이어 분리
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParticipantTrackingBar(
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
private fun ParticipantMapMarker(
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
            configuration.screenHeightDp.dp.toPx()
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
    // 더미 참가자 데이터 (테스트용)
    val dummyParticipants = listOf(
        com.teammanduk.adego.core.model.Participant(
            userId = "user1",
            name = "김철수",
            profileColor = "#E53935",
            location = com.teammanduk.adego.core.model.ParticipantLocation(
                latitude = 37.5665,
                longitude = 126.9780,
                updatedAt = System.currentTimeMillis()
            ),
            distanceToDestination = 1500,
            route = null
        ),
        com.teammanduk.adego.core.model.Participant(
            userId = "user2",
            name = "이영희",
            profileColor = "#43A047",
            location = com.teammanduk.adego.core.model.ParticipantLocation(
                latitude = 37.5675,
                longitude = 126.9790,
                updatedAt = System.currentTimeMillis()
            ),
            distanceToDestination = 800,
            route = null
        ),
        com.teammanduk.adego.core.model.Participant(
            userId = "user3",
            name = "박민수",
            profileColor = "#1E88E5",
            location = com.teammanduk.adego.core.model.ParticipantLocation(
                latitude = 37.5655,
                longitude = 126.9770,
                updatedAt = System.currentTimeMillis()
            ),
            distanceToDestination = 2000,
            route = null
        )
    )

    val dummyDestination = com.teammanduk.adego.core.model.Place(
        name = "스타벅스 명동점",
        address = "서울시 중구 명동",
        latitude = 37.5665,
        longitude = 126.9780
    )

    AdegoTheme {
        MapScreen(
            roomId = "preview123",
            meetingTime = "14시 30분",
            participants = dummyParticipants,
            selectedParticipantIndex = 0,
            onAction = {},
            destination = dummyDestination,
            currentUserId = "user1",
            initialLocation = dummyParticipants[0].location!!,
            showInviteDialog = false
        )
    }
}
