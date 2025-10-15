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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.teammanduk.adego.core.designsystem.ui.theme.AdegoTheme
import com.teammanduk.adego.core.ui.component.LoadingScreen
import com.teammanduk.adego.feature.map.Participant
import com.teammanduk.adego.feature.map.component.InviteDialog
import com.teammanduk.adego.feature.map.component.ParticipantCardPager
import com.teammanduk.adego.feature.map.component.TopInfoSection
import com.teammanduk.adego.feature.map.createParticipantMarkerIcon
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
        MapScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onInviteClick = { showInviteDialog = true },
            onNavigateToSelectStartPlace = onNavigateToSelectStartPlace
        )
    }

    if (showInviteDialog) {
        InviteDialog(
            inviteCode = uiState.room?.roomId ?: "",
            onDismiss = { showInviteDialog = false }
        )
    }
}

@Composable
private fun MapScreen(
    uiState: MapUiState,
    onAction: (MapIntent) -> Unit,
    onInviteClick: () -> Unit,
    onNavigateToSelectStartPlace: (String, String, Double, Double) -> Unit = { _, _, _, _ -> }
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

            // 선택한 경로 그리기
            if (uiState.selectedRouteIndex != null) {
                val selectedRoute = uiState.searchedRoutes.getOrNull(uiState.selectedRouteIndex)
                selectedRoute?.subPaths?.forEach { subPath ->
                    // 각 SubPath의 시작점과 끝점을 연결
                    val startLat = subPath.startLatitude
                    val startLng = subPath.startLongitude
                    val endLat = subPath.endLatitude
                    val endLng = subPath.endLongitude

                    if (startLat != null && startLng != null && endLat != null && endLng != null) {
                        val points = mutableListOf<LatLng>()
                        points.add(LatLng(startLat, startLng))

                        // 경유 정류장이 있으면 추가
                        subPath.passStations?.forEach { station ->
                            points.add(LatLng(station.latitude, station.longitude))
                        }

                        points.add(LatLng(endLat, endLng))

                        // 교통수단에 따라 다른 색상 사용
                        val lineColor = when (subPath.trafficType) {
                            com.teammanduk.adego.core.model.TrafficType.SUBWAY -> androidx.compose.ui.graphics.Color(0xFF0052A4)
                            com.teammanduk.adego.core.model.TrafficType.BUS -> androidx.compose.ui.graphics.Color(0xFF53B332)
                            com.teammanduk.adego.core.model.TrafficType.WALK -> androidx.compose.ui.graphics.Color.Gray
                        }

                        Polyline(
                            points = points,
                            color = lineColor,
                            width = 10f
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
                participants = uiState.participants,
                pagerState = pagerState
            )
        }
    }
}
