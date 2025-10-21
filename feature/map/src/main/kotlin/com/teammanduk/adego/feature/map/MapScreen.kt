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
    selectedRoute: com.teammanduk.adego.core.model.Route? = null
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

    // 선택된 경로가 있으면 ViewModel에 설정
    LaunchedEffect(selectedRoute) {
        selectedRoute?.let {
            viewModel.setSelectedRoute(it)
        }
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

            uiState.currentUser?.location == null -> {
                LoadingScreen(text = "현재 위치를 가져오는 중...")
            }

            else -> {
                MapScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    onInviteClick = { showInviteDialog = true },
                    onNavigateToSelectStartPlace = onNavigateToSelectStartPlace
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
) {
    // UI 상태
    val pagerState = rememberPagerState(
        pageCount = { uiState.participants.size }, initialPage = uiState.selectedParticipantIndex
    )
    val coroutineScope = rememberCoroutineScope()
    var isCameraInitialized by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        // currentUser 위치를 우선적으로 사용하고, 없으면 선택된 참가자 위치 사용
        val initialLocation = uiState.currentUser?.location
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

            // 선택된 참가자의 경로 그리기 (서버에서 받은 selectedRoute 우선 사용)
            val selectedParticipant = uiState.participants.getOrNull(pagerState.currentPage)
            selectedParticipant?.route?.selectedRoute?.let { route ->
                // 모든 SubPath를 미리 처리하여 Polyline 데이터 생성 (remember로 메모이제이션)
                val polylineDataList = remember(route) {
                    val list = mutableListOf<PolylineData>()
                    var lastPoint: LatLng? = null

                    route.subPaths.forEachIndexed { index, subPath ->
                        val points = mutableListOf<LatLng>()
                        val graphicData = subPath.graphicData

                        // TMAP graphicData 사용 (실제 경로)
                        if (!graphicData.isNullOrEmpty()) {
                            // 이전 SubPath와 연결이 끊긴 경우 연결선 추가
                            if (lastPoint != null) {
                                val firstPoint = LatLng(
                                    graphicData.first().latitude, graphicData.first().longitude
                                )
                                val distance = calculateDistance(lastPoint!!, firstPoint)

                                // 50미터 이상 떨어져 있으면 연결선 그리기 (반투명 회색)
                                if (distance > 50) {
                                    list.add(
                                        PolylineData(
                                            points = listOf(lastPoint!!, firstPoint),
                                            color = Color.Gray.copy(
                                                alpha = 0.4f
                                            ),
                                            width = 4f,
                                            key = "connector_$index"
                                        )
                                    )
                                }
                            }

                            // graphicData의 모든 좌표를 points에 추가
                            graphicData.forEach { coord ->
                                points.add(LatLng(coord.latitude, coord.longitude))
                            }

                            lastPoint = points.lastOrNull()
                        } else {
                            // graphicData가 없는 경우: 시작-끝 직선으로 대체
                            val startLat = subPath.startLatitude
                            val startLng = subPath.startLongitude
                            val endLat = subPath.endLatitude
                            val endLng = subPath.endLongitude

                            if (startLat != null && startLng != null && endLat != null && endLng != null) {
                                val startPoint = LatLng(startLat, startLng)
                                val endPoint = LatLng(endLat, endLng)

                                // 이전 SubPath와 연결
                                if (lastPoint != null && lastPoint != startPoint) {
                                    points.add(lastPoint!!)
                                }

                                points.add(startPoint)
                                points.add(endPoint)
                                lastPoint = endPoint
                            }
                        }

                        // 포인트가 있으면 Polyline 데이터 추가
                        if (points.size >= 2) {
                            // 교통수단에 따라 다른 색상과 두께 사용
                            val lineColor = when (subPath.trafficType) {
                                com.teammanduk.adego.core.model.TrafficType.SUBWAY -> Color(
                                    0xFF0052A4
                                )

                                com.teammanduk.adego.core.model.TrafficType.BUS -> Color(
                                    0xFF53B332
                                )

                                com.teammanduk.adego.core.model.TrafficType.WALK -> Color(
                                    0xFF808080
                                )
                            }

                            val lineWidth = when (subPath.trafficType) {
                                com.teammanduk.adego.core.model.TrafficType.SUBWAY -> 12f
                                com.teammanduk.adego.core.model.TrafficType.BUS -> 10f
                                com.teammanduk.adego.core.model.TrafficType.WALK -> 6f
                            }

                            list.add(
                                PolylineData(
                                    points = points.toList(),
                                    color = lineColor,
                                    width = lineWidth,
                                    key = "subpath_$index"
                                )
                            )
                        }
                    }

                    list.toList()
                }

                // 생성된 Polyline 데이터를 기반으로 실제 Polyline 그리기
                polylineDataList.forEach { data ->
                    key(data.key) {
                        Polyline(
                            points = data.points, color = data.color, width = data.width
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
            if (uiState.selectedRouteIndex != null) {
                // 경로 선택 후: 아이콘 버튼으로 최소화 (오른쪽 배치)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
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
