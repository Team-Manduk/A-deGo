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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

            // 경로 설정 다이얼로그
            if (uiState.showRouteDialog) {
                // 경로 검색 결과가 있으면 경로 목록 표시, 없으면 경로 설정 다이얼로그 표시
                if (uiState.searchedRoutes.isNotEmpty()) {
                    RouteListDialog(
                        routes = uiState.searchedRoutes,
                        selectedRouteIndex = uiState.selectedRouteIndex,
                        onRouteSelect = { index -> viewModel.onAction(MapIntent.SelectRoute(index)) },
                        onDismiss = { viewModel.onAction(MapIntent.DismissRouteDialog) }
                    )
                } else {
                    RouteSetupDialog(
                        destination = uiState.room?.destination,
                        startPlace = uiState.startPlace,
                        isSearchingRoute = uiState.isSearchingRoute,
                        onDismiss = { viewModel.onAction(MapIntent.DismissRouteDialog) },
                        onSelectStartPlace = { viewModel.onAction(MapIntent.ShowSelectStartPlace) },
                        onSearchRoute = { viewModel.onAction(MapIntent.SearchRoute) }
                    )
                }
            }

            // 출발지 선택 화면
            if (uiState.showSelectStartPlace) {
                val selectPlaceViewModel: com.teammanduk.adego.feature.place.SelectPlaceViewModel =
                    androidx.hilt.navigation.compose.hiltViewModel()
                val selectedPlace by selectPlaceViewModel.currentSearchResult.collectAsState()

                com.teammanduk.adego.feature.place.SelectPlaceRoute(
                    title = "출발지 선택",
                    buttonText = "이 위치에서 출발",
                    showTopBar = true,
                    onBackClick = { viewModel.onAction(MapIntent.DismissSelectStartPlace) },
                    onPlaceSelected = {
                        // 장소 선택 확정 시에만 MapViewModel에 전달하고 화면 닫기
                        selectedPlace?.let {
                            viewModel.onAction(MapIntent.StartPlaceSelected(it))
                        }
                        viewModel.onAction(MapIntent.DismissSelectStartPlace)
                    },
                    onSearchClick = { viewModel.onAction(MapIntent.ShowSearchPlace) },
                    viewModel = selectPlaceViewModel
                )
            }

            // 장소 검색 화면
            if (uiState.showSearchPlace) {
                com.teammanduk.adego.feature.place.SearchPlaceRoute(
                    onBackClick = { viewModel.onAction(MapIntent.DismissSearchPlace) },
                    onPlaceClick = { viewModel.onAction(MapIntent.DismissSearchPlace) }
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
            // 선택된 경로를 폴리라인으로 표시
            if (uiState.selectedRouteIndex != null && uiState.searchedRoutes.isNotEmpty()) {
                val selectedRoute = uiState.searchedRoutes.getOrNull(uiState.selectedRouteIndex)
                selectedRoute?.let { route ->
                    // 출발지와 목적지 좌표로 간단한 폴리라인 그리기
                    uiState.startPlace?.let { start ->
                        uiState.room?.destination?.let { dest ->
                            com.google.maps.android.compose.Polyline(
                                points = listOf(
                                    LatLng(start.latitude, start.longitude),
                                    LatLng(dest.latitude, dest.longitude)
                                ),
                                color = AdegoTheme.colors.main500,
                                width = 10f
                            )
                        }
                    }
                }
            }

            // 목적지 마커 추가
            uiState.room?.destination?.let {
                com.google.maps.android.compose.Marker(
                    state = com.google.maps.android.compose.rememberMarkerState(
                        position = LatLng(it.latitude, it.longitude)
                    ),
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
