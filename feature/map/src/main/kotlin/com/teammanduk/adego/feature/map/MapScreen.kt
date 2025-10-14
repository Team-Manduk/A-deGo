package com.teammanduk.adego.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.ui.component.LoadingScreen
import com.teammanduk.adego.feature.map.component.InviteDialog
import com.teammanduk.adego.feature.map.component.ParticipantCardPager
import com.teammanduk.adego.feature.map.component.ParticipantMapMarker
import com.teammanduk.adego.feature.map.component.TopInfoSection
import com.teammanduk.adego.feature.map.component.createParticipantMarkerIcon
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapUiState
import com.teammanduk.adego.feature.map.model.Participant
import kotlinx.coroutines.launch

@Composable
internal fun MapRoute(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocationTracking()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    if (!uiState.isInitialLocationLoaded || uiState.participants.isEmpty()) {
        LoadingScreen(text = "현재 위치를 가져오는 중...")
    } else {
        val currentUser = uiState.participants.find { it.userId == viewModel.userId }
        if (currentUser?.location != null) {
            MapScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
                onInviteClick = { showInviteDialog = true }
            )

            if (showInviteDialog) {
                InviteDialog(
                    inviteCode = uiState.room?.roomId ?: "",
                    onDismiss = { showInviteDialog = false }
                )
            }
        } else {
            LoadingScreen(text = "사용자 위치를 확인 중...")
        }
    }
}

@Composable
private fun MapScreen(
    uiState: MapUiState,
    onAction: (MapIntent) -> Unit,
    onInviteClick: () -> Unit
) {
    // UI 상태
    val pagerState = rememberPagerState(
        pageCount = { uiState.participants.size },
        initialPage = uiState.selectedParticipantIndex
    )
    val coroutineScope = rememberCoroutineScope()
    var isCameraInitialized by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        val initialLocation =
            uiState.participants.getOrNull(uiState.selectedParticipantIndex)?.location
        if (initialLocation != null) {
            position = CameraPosition.fromLatLngZoom(
                LatLng(initialLocation.latitude, initialLocation.longitude),
                15f
            )
        }
    }

    // 동기화 로직: pagerState <-> ViewModel
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.selectedParticipantIndex) {
            onAction(MapIntent.SelectParticipant(pagerState.currentPage))
        }
    }

    LaunchedEffect(uiState.selectedParticipantIndex) {
        if (pagerState.currentPage != uiState.selectedParticipantIndex) {
            pagerState.animateScrollToPage(uiState.selectedParticipantIndex)
        }
    }

    // 참가자 선택 시 카메라 이동
    LaunchedEffect(uiState.selectedParticipantIndex) {
        if (isCameraInitialized) {
            val selectedParticipant =
                uiState.participants.getOrNull(uiState.selectedParticipantIndex)
            selectedParticipant?.location?.let { location ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f
                    ),
                    durationMs = 500
                )
            }
        } else {
            isCameraInitialized = true
        }
    }

    // 레이아웃
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // 지도
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // 목적지 마커
            uiState.room?.destination?.let {
                Marker(
                    state = rememberMarkerState(position = LatLng(it.latitude, it.longitude)),
                    title = it.name,
                    snippet = "목적지"
                )
            }

            // 참가자 마커들
            uiState.participants.forEach { participant ->
                key(participant.userId) {
                    participant.location?.let { location ->
                        val isSelected =
                            uiState.participants.getOrNull(pagerState.currentPage)?.userId == participant.userId

                        val markerIcon = createParticipantMarkerIcon(participant.color, isSelected)

                        Marker(
                            state = rememberMarkerState(
                                position = LatLng(location.latitude, location.longitude)
                            ),
                            title = participant.name,
                            icon = markerIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            zIndex = if (isSelected) 1f else 0f
                        )
                    }
                }
            }
        }

        // 상단 정보 영역
        TopInfoSection(
            meetingTime = uiState.room?.meetingTime ?: "00시 00분",
            participantCount = uiState.participants.size,
            participants = uiState.participants,
            destination = uiState.room?.destination,
            pagerState = pagerState,
            onInviteClick = onInviteClick,
            onParticipantClick = { index ->
                onAction(MapIntent.SelectParticipant(index))
                coroutineScope.launch {
                    uiState.participants.getOrNull(index)?.location?.let { location ->
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                15f
                            ),
                            durationMs = 500
                        )
                    }
                }
            },
            onDestinationClick = {
                coroutineScope.launch {
                    uiState.room?.destination?.let { destination ->
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(destination.latitude, destination.longitude),
                                15f
                            ),
                            durationMs = 500
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 화면 밖 참가자 마커
        uiState.participants.forEachIndexed { index, participant ->
            key(participant.userId) {
                ParticipantMapMarker(
                    participant = participant,
                    isSelected = pagerState.currentPage == index,
                    cameraPositionState = cameraPositionState
                )
            }
        }

        // 하단 영역: 경로 설정 버튼 + 참가자 카드
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 경로 설정 버튼
            Button(
                onClick = { onAction(MapIntent.ShowRouteDialog) },
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.highlight500
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "목적지까지 경로 설정",
                    style = AdegoTheme.typography.bodyLarge,
                    color = Color.White
                )
            }

            // 참가자 카드 (스와이프)
            ParticipantCardPager(
                participants = uiState.participants,
                pagerState = pagerState,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
@Composable
private fun ParticipantMapMarker(
    participant: com.teammanduk.adego.core.model.Participant,
    isSelected: Boolean,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState
) {
    participant.location?.let { location ->
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

        // 화면 밖인 경우만 오버레이로 표시
        if (!isOnScreen) {
            screenPosition?.let { screenPos ->
                // 참가자 색상 파싱
                val participantColor = try {
                    Color(android.graphics.Color.parseColor(participant.profileColor))
                } catch (e: Exception) {
                    Color(0xFFE53935)
                }

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
private fun RouteSetupDialog(
    destination: com.teammanduk.adego.core.model.Place?,
    onDismiss: () -> Unit
) {
    var startLocation by remember { mutableStateOf("") }

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
                    text = "경로 설정",
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

            // 출발지 입력
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "출발지",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.onBackground
                )

                OutlinedTextField(
                    value = startLocation,
                    onValueChange = { startLocation = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "출발지를 입력하세요 (입력 기능 미구현)",
                            style = AdegoTheme.typography.bodyLarge,
                            color = Color(0xFF9E9E9E)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = false // 입력 기능 미구현
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 목적지 표시 (자동 설정됨)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "목적지",
                    style = AdegoTheme.typography.bodyLarge,
                    color = AdegoTheme.colors.onBackground
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = AdegoTheme.colors.main500,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = AdegoTheme.colors.main500.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "목적지",
                            tint = AdegoTheme.colors.main500,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = destination?.name ?: "목적지 없음",
                                style = AdegoTheme.typography.bodyLarge,
                                color = AdegoTheme.colors.onBackground
                            )
                            destination?.address?.let { address ->
                                Text(
                                    text = address,
                                    style = AdegoTheme.typography.bodySmall,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 경로 검색 버튼 (비활성화)
            Button(
                onClick = { /* TODO: 경로 검색 기능 구현 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = false // 입력 기능이 구현되면 활성화
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "경로 검색 (준비중)",
                    style = AdegoTheme.typography.titleLarge,
                    color = Color.White
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

// Preview 주석 처리 - MapScreen의 signature 변경으로 인해 일시적으로 비활성화
// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun MapScreenPreview() {
//     ...
// }
