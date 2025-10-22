package com.teammanduk.adego.feature.map

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.teammanduk.adego.core.ui.component.LoadingScreen
import com.teammanduk.adego.core.ui.permission.PermissionRequester
import com.teammanduk.adego.core.ui.permission.PermissionType
import com.teammanduk.adego.feature.map.component.ErrorScreen
import com.teammanduk.adego.feature.map.component.InviteDialog
import com.teammanduk.adego.feature.map.component.ParticipantCardPager
import com.teammanduk.adego.feature.map.component.TopInfoSection
import com.teammanduk.adego.feature.map.component.UserNameInputDialog
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapSideEffect
import com.teammanduk.adego.feature.map.model.MapUiState
import kotlinx.coroutines.launch

@Composable
internal fun MapRoute(
    viewModel: MapViewModel = hiltViewModel(),
    onNavigateToSelectStartPlace: (String, String, Double, Double) -> Unit = { _, _, _, _ -> },
    onNavigateToHome: () -> Unit = {},
    onNavigateToRouteGuidance: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }
    var backPressedTime by remember { mutableLongStateOf(0L) }

    // 백버튼 더블탭 종료 (세션 참가 중에는 앱 종료만 가능)
    // TODO: 추후 "일시 나가기" 버튼 추가 시
    //  - 위치: 상단 앱바 또는 메뉴
    //  - 동작: 세션 유지하고 Home으로 이동
    //  - 구현: onNavigateToHome() 호출
    //  - 필요: Home 화면에 "진행 중인 방" 표시 + "돌아가기" 버튼
    BackHandler {
        val currentTime = System.currentTimeMillis()

        if (currentTime - backPressedTime < 2000) {
            // 2초 내 재입력 → 앱 종료
            (context as? Activity)?.finish()
        } else {
            // 첫 번째 입력 → 토스트 표시
            backPressedTime = currentTime
            Toast.makeText(
                context,
                "한 번 더 누르면 종료됩니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // SideEffect 처리
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is MapSideEffect.NavigateToHome -> {
                    onNavigateToHome()
                }
            }
        }
    }

    // selectedRoute 파라미터 제거됨 - Room DB에서 자동 조회

    // 최소 로딩 시간 보장 (자연스러운 UX)
    var isMinimumLoadingTimePassed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400) // 0.4초 최소 로딩 시간
        isMinimumLoadingTimePassed = true
    }

    PermissionRequester(
        permissionTypes = listOf(PermissionType.Location)
    ) {
        when {
            uiState.isCheckingRoom -> {
                LoadingScreen(text = "방 정보를 확인하는 중...")
            }

            uiState.error != null -> {
                // 방 존재 여부 확인 실패 등의 치명적 에러
                ErrorScreen(
                    message = uiState.error!!,
                    onAction = { viewModel.onAction(MapIntent.NavigateToHome) }
                )
            }

            uiState.showUserNameDialog -> {
                UserNameInputDialog(
                    userName = uiState.userName,
                    onUserNameChange = { viewModel.onAction(MapIntent.UpdateUserName(it)) },
                    onConfirm = { viewModel.onAction(MapIntent.ConfirmUserName) },
                    error = uiState.error,
                    onGenerateRandomName = { viewModel.onAction(MapIntent.GenerateRandomName) }
                )
            }

            !isMinimumLoadingTimePassed || uiState.myCurrentLocation == null -> {
                LoadingScreen(text = "현재 위치를 가져오는 중...")
            }

            else -> {
                android.util.Log.d("MapPerformance", "[6] MapScreen composing at ${System.currentTimeMillis()}")
                MapScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    onInviteClick = { showInviteDialog = true },
                    onNavigateToSelectStartPlace = onNavigateToSelectStartPlace,
                    onNavigateToRouteGuidance = onNavigateToRouteGuidance
                )

                if (showInviteDialog) {
                    InviteDialog(
                        inviteCode = uiState.room?.roomId ?: "",
                        roomName = uiState.room?.roomName ?: "",
                        destinationName = uiState.room?.destination?.name ?: "",
                        meetingTime = uiState.room?.meetingTime ?: "",
                        onDismiss = { showInviteDialog = false })
                }
            }
        }
    }
}

@Composable
private fun MapScreen(
    uiState: MapUiState,
    onAction: (MapIntent) -> Unit,
    onInviteClick: () -> Unit,
    onNavigateToSelectStartPlace: (String, String, Double, Double) -> Unit = { _, _, _, _ -> },
    onNavigateToRouteGuidance: (String, String) -> Unit = { _, _ -> }
) {
    // UI 상태
    val pagerState = rememberPagerState(
        pageCount = { uiState.participants.size }, initialPage = uiState.selectedParticipantIndex
    )
    val coroutineScope = rememberCoroutineScope()
    var isCameraInitialized by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        // 내 현재 위치(GPS 직접)를 우선 사용하고, 없으면 선택된 참가자 위치 사용
        val initialLocation = uiState.myCurrentLocation
            ?: uiState.participants.getOrNull(uiState.selectedParticipantIndex)?.location
        if (initialLocation != null) {
            position = CameraPosition.fromLatLngZoom(
                LatLng(initialLocation.latitude, initialLocation.longitude), 15f
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
                        LatLng(location.latitude, location.longitude), 15f
                    ), durationMs = 500
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
        LaunchedEffect(Unit) {
            android.util.Log.d("MapPerformance", "[7] GoogleMap composing at ${System.currentTimeMillis()}")
        }

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

            // 내 위치 마커 (GPS에서 직접 받은 위치 사용)
            uiState.myCurrentLocation?.let { myLocation ->
                val currentUser = uiState.currentUser
                if (currentUser != null) {
                    key("my_location") {
                        val isSelected =
                            uiState.participants.getOrNull(pagerState.currentPage)?.userId == uiState.userId

                        val markerIcon = createParticipantMarkerIcon(currentUser.color, isSelected)

                        val markerState = rememberMarkerState(
                            key = "my_${myLocation.latitude}_${myLocation.longitude}",
                            position = LatLng(myLocation.latitude, myLocation.longitude)
                        )

                        // 위치가 변경되면 MarkerState 업데이트
                        LaunchedEffect(myLocation.latitude, myLocation.longitude) {
                            markerState.position = LatLng(myLocation.latitude, myLocation.longitude)
                        }

                        Marker(
                            state = markerState,
                            title = currentUser.name,
                            icon = markerIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            zIndex = if (isSelected) 1f else 0f
                        )
                    }
                }
            }

            // 다른 참가자 마커들 (Firebase에서 받은 위치 사용)
            uiState.participants.filter { it.userId != uiState.userId }.forEach { participant ->
                key(participant.userId) {
                    participant.location?.let { location ->
                        val isSelected =
                            uiState.participants.getOrNull(pagerState.currentPage)?.userId == participant.userId

                        val markerIcon = createParticipantMarkerIcon(participant.color, isSelected)

                        val markerState = rememberMarkerState(
                            key = "${participant.userId}_${location.latitude}_${location.longitude}",
                            position = LatLng(location.latitude, location.longitude)
                        )

                        // 위치가 변경되면 MarkerState 업데이트
                        LaunchedEffect(location.latitude, location.longitude) {
                            markerState.position = LatLng(location.latitude, location.longitude)
                        }

                        Marker(
                            state = markerState,
                            title = participant.name,
                            icon = markerIcon,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            zIndex = if (isSelected) 1f else 0f
                        )
                    }
                }
            }

            // 선택된 참가자의 경로 그리기 (polyline 디코딩)
            val selectedParticipant = uiState.participants.getOrNull(pagerState.currentPage)
            selectedParticipant?.route?.polyline?.let { polylineString ->
                if (polylineString.isNotEmpty()) {
                    // polyline 디코딩하여 좌표 리스트 생성
                    val points = remember(polylineString) {
                        try {
                            com.teammanduk.adego.core.domain.util.PolylineEncoder.decode(polylineString)
                                .map { (lat, lng) -> LatLng(lat, lng) }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }

                    // Polyline 그리기
                    if (points.size >= 2) {
                        Polyline(
                            points = points,
                            color = Color(0xFF4285F4), // 구글 블루
                            width = 10f
                        )
                    }
                }
            }
        }

        // 상단 정보 영역 + 방 나가기 버튼
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
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
                                    LatLng(location.latitude, location.longitude), 15f
                                ), durationMs = 500
                            )
                        }
                    }
                },
                onDestinationClick = {
                    coroutineScope.launch {
                        uiState.room?.destination?.let { destination ->
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(destination.latitude, destination.longitude), 15f
                                ), durationMs = 500
                            )
                        }
                    }
                }
            )

            // 방 나가기 버튼 (TopInfoSection 바로 아래)
            IconButton(
                onClick = { onAction(MapIntent.LeaveRoom) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, end = 16.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "방 나가기",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 하단 영역: 경로 선택 버튼 + 참가자 카드
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // 경로 선택 버튼 - 경로 선택 여부에 따라 다른 UI
            // 로컬 경로가 있거나, 서버에서 받아온 내 경로(polyline)가 있으면 축소
            val hasRoute = uiState.selectedRouteIndex != null ||
                           uiState.currentUser?.route?.polyline?.isNotEmpty() == true

            if (hasRoute) {
                // 경로 선택 후: 경로 안내 버튼 + 경로 변경 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 경로 안내 버튼 (왼쪽, 확장)
                    Button(
                        onClick = {
                            onNavigateToRouteGuidance(uiState.roomId, uiState.userId)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE)
                        )
                    ) {
                        Text(text = "경로 안내")
                    }

                    // 경로 변경 버튼 (오른쪽, 고정 크기)
                    FloatingActionButton(
                        onClick = {
                            val destination = uiState.room?.destination
                            if (destination != null) {
                                onNavigateToSelectStartPlace(
                                    uiState.roomId,
                                    uiState.userId,
                                    destination.latitude,
                                    destination.longitude
                                )
                            }
                        },
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "경로 변경"
                        )
                    }
                }
            } else {
                // 경로 선택 전: 전체 너비 버튼 (카드뷰와 너비 맞춤)
                Button(
                    onClick = {
                        val destination = uiState.room?.destination
                        if (destination != null) {
                            onNavigateToSelectStartPlace(
                                uiState.roomId,
                                uiState.userId,
                                destination.latitude,
                                destination.longitude
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = "경로 선택")
                }
            }

            // 참가자 카드
            ParticipantCardPager(
                participants = uiState.participants, pagerState = pagerState
            )
        }
    }
}

/**
 * Polyline 데이터를 저장하는 데이터 클래스
 */
private data class PolylineData(
    val points: List<LatLng>, val color: Color, val width: Float, val key: String
)

/**
 * 두 LatLng 포인트 간의 거리를 미터 단위로 계산합니다 (Haversine formula).
 */
private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
    val earthRadius = 6371000.0 // 지구 반지름 (미터)

    val lat1Rad = Math.toRadians(point1.latitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
    val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

    val a =
        Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(
            deltaLng / 2
        ) * Math.sin(deltaLng / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return earthRadius * c
}
