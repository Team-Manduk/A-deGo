package com.teammanduk.adego.feature.map

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
import com.teammanduk.adego.feature.map.component.InviteDialog
import com.teammanduk.adego.feature.map.component.ParticipantCardPager
import com.teammanduk.adego.feature.map.component.TopInfoSection
import com.teammanduk.adego.feature.map.component.UserNameInputDialog
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapUiState
import kotlinx.coroutines.launch

@Composable
internal fun MapRoute(
    viewModel: MapViewModel = hiltViewModel(),
    onNavigateToSelectStartPlace: (String, String, Double, Double) -> Unit = { _, _, _, _ -> },
    selectedRoute: com.teammanduk.adego.core.model.Route? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }

    // 선택된 경로가 있으면 ViewModel에 설정
    LaunchedEffect(selectedRoute) {
        selectedRoute?.let {
            viewModel.setSelectedRoute(it)
        }
    }

    PermissionRequester(
        permissionTypes = listOf(PermissionType.Location),
        onGranted = { viewModel.startLocationTracking() }
    ) {
        MapRouteContent(
            uiState = uiState,
            viewModel = viewModel,
            showInviteDialog = showInviteDialog,
            onShowInviteDialogChange = { showInviteDialog = it },
            onNavigateToSelectStartPlace = onNavigateToSelectStartPlace
        )
    }
}

@Composable
private fun MapRouteContent(
    uiState: MapUiState,
    viewModel: MapViewModel,
    showInviteDialog: Boolean,
    onShowInviteDialogChange: (Boolean) -> Unit,
    onNavigateToSelectStartPlace: (String, String, Double, Double) -> Unit = { _, _, _, _ -> }
) {
    when {
        uiState.showUserNameInput -> {
            UserNameInputDialog(
                userName = uiState.userName,
                onUserNameChange = { viewModel.onAction(MapIntent.UpdateUserName(it)) },
                onConfirm = { viewModel.onAction(MapIntent.ConfirmUserName) },
                error = uiState.error,
                onGenerateRandomName = { viewModel.onAction(MapIntent.GenerateRandomName) }
            )
        }

        uiState.myLocation == null -> {
            LoadingScreen(text = "현재 위치를 가져오는 중...")
        }

        uiState.participants.isEmpty() -> {
            LoadingScreen(text = "방 정보를 불러오는 중...")
        }

        else -> {
            MapScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
                onInviteClick = { onShowInviteDialogChange(true) },
                onNavigateToSelectStartPlace = onNavigateToSelectStartPlace
            )

            if (showInviteDialog) {
                InviteDialog(
                    inviteCode = uiState.room?.roomId ?: "",
                    roomName = uiState.room?.roomName ?: "",
                    destinationName = uiState.room?.destination?.name ?: "",
                    meetingTime = uiState.room?.meetingTime ?: "",
                    onDismiss = { onShowInviteDialogChange(false) })
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
        val initialLocation =
            uiState.participants.getOrNull(uiState.selectedParticipantIndex)?.location
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

            // 선택한 경로 그리기 (TMAP Transit API 방식)
            Log.d(
                "MapScreen",
                "경로 그리기 체크 - selectedRouteIndex: ${uiState.selectedRouteIndex}, searchedRoutes size: ${uiState.searchedRoutes.size}"
            )
            if (uiState.selectedRouteIndex != null) {
                val selectedRoute = uiState.searchedRoutes.getOrNull(uiState.selectedRouteIndex)
                Log.d(
                    "MapScreen",
                    "selectedRoute: ${if (selectedRoute != null) "존재 (subPaths: ${selectedRoute.subPaths.size})" else "null"}"
                )

                selectedRoute?.let { route ->
                    Log.d("MapScreen", "경로 그리기 시작 - subPaths: ${route.subPaths.size}개")

                    // 모든 SubPath를 미리 처리하여 Polyline 데이터 생성 (remember로 메모이제이션)
                    val polylineDataList = remember(route) {
                        val list = mutableListOf<PolylineData>()
                        var lastPoint: LatLng? = null

                        route.subPaths.forEachIndexed { index, subPath ->
                            val points = mutableListOf<LatLng>()
                            val graphicData = subPath.graphicData

                            Log.d(
                                "MapScreen",
                                "SubPath[$index] 처리 - trafficType: ${subPath.trafficType}, graphicData: ${graphicData?.size ?: 0}개"
                            )

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

                                Log.d(
                                    "MapScreen",
                                    "SubPath[$index] Polyline 데이터 생성 - points: ${points.size}개, color: $lineColor"
                                )
                                if (index == 0 && points.isNotEmpty()) {
                                    Log.d("MapScreen", "  첫 번째 경로의 첫 좌표: ${points.first()}")
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

                        Log.d("MapScreen", "총 ${list.size}개의 Polyline 데이터 생성 완료")
                        list.toList()
                    }

                    // 생성된 Polyline 데이터를 기반으로 실제 Polyline 그리기
                    Log.d("MapScreen", "Polyline 렌더링 시작 - ${polylineDataList.size}개")
                    polylineDataList.forEach { data ->
                        key(data.key) {
                            Polyline(
                                points = data.points, color = data.color, width = data.width
                            )
                        }
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
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 하단 영역: 경로 선택 버튼 + 참가자 카드
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // 경로 선택 버튼
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
                    .padding(horizontal = 16.dp)
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
