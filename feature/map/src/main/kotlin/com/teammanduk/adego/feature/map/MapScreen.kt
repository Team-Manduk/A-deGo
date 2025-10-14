package com.teammanduk.adego.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapUiState
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

    when {
        uiState.showUserNameInput -> {
            UserNameInputDialog(
                userName = uiState.userName,
                onUserNameChange = { viewModel.onAction(MapIntent.UpdateUserName(it)) },
                onConfirm = { viewModel.onAction(MapIntent.ConfirmUserName) },
                error = uiState.error
            )
        }

        uiState.participants.isEmpty() ||
        uiState.participants.getOrNull(uiState.selectedParticipantIndex)?.location == null -> {
            LoadingScreen(text = "현재 위치를 가져오는 중...")
        }

        else -> {
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
                            anchor = Offset(0.5f, 0.5f),
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

        ParticipantCardPager(
            participants = uiState.participants,
            pagerState = pagerState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun UserNameInputDialog(
    userName: String,
    onUserNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    error: String?
) {
    Dialog(onDismissRequest = { }) {
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
                    text = "사용자 이름 입력",
                    style = AdegoTheme.typography.titleLarge,
                    color = AdegoTheme.colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 이름 입력 필드
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "이름을 입력해주세요",
                            style = AdegoTheme.typography.bodyLarge,
                            color = AdegoTheme.colors.line500
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = Color.Red,
                        style = AdegoTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 확인 버튼
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdegoTheme.colors.main500
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = userName.isNotBlank()
            ) {
                Text(
                    text = "확인",
                    style = AdegoTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }
}
