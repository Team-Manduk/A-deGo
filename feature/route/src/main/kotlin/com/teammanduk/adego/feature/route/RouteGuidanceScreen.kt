package com.teammanduk.adego.feature.route

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
internal fun RouteGuidanceRoute(
    onNavigateBack: () -> Unit,
    viewModel: RouteGuidanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RouteGuidanceScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteGuidanceScreen(
    uiState: RouteGuidanceUiState,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("경로 안내") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.error ?: "알 수 없는 오류가 발생했습니다")
                }
            }
            uiState.route != null -> {
                val context = LocalContext.current
                val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
                var myLocation by remember { mutableStateOf<LatLng?>(null) }

                val allPoints = uiState.route!!.calculateAllPoints()
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(allPoints.center(), 14f)
                }

                // 내 위치 가져오기
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                        // 권한 허용됨
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    myLocation = LatLng(it.latitude, it.longitude)
                                }
                            }
                        } catch (e: SecurityException) {
                            // 권한 없음
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    myLocation = LatLng(it.latitude, it.longitude)
                                }
                            }
                        } catch (e: SecurityException) {
                            // 권한 없음
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                RoutePreviewScreen(
                    route = uiState.route!!,
                    isLoadingDetails = false,
                    onConfirm = { /* 경로 안내 중에는 확인 버튼 필요 없음 */ },
                    onCancel = onNavigateBack,
                    modifier = Modifier.padding(paddingValues),
                    mode = RouteScreenMode.GUIDANCE,
                    cameraPositionState = cameraPositionState,
                    onMyLocationClick = {
                        myLocation?.let { location ->
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(location, 16f),
                                    durationMs = 500
                                )
                            }
                        }
                    }
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "선택된 경로가 없습니다")
                }
            }
        }
    }
}
