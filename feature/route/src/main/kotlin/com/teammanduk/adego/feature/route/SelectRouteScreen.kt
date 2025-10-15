package com.teammanduk.adego.feature.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType

@Composable
internal fun SelectRouteRoute(
    onNavigateBack: () -> Unit,
    onRouteSelected: (Route) -> Unit,
    viewModel: SelectRouteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SelectRouteScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRouteClick = { index ->
            viewModel.selectRoute(index)
        },
        onConfirmRoute = {
            uiState.selectedRouteIndex?.let { index ->
                uiState.routes.getOrNull(index)?.let { route ->
                    onRouteSelected(route)
                }
            }
        },
        onCancelSelection = {
            viewModel.deselectRoute()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectRouteScreen(
    uiState: SelectRouteUiState,
    onNavigateBack: () -> Unit,
    onRouteClick: (Int) -> Unit,
    onConfirmRoute: () -> Unit,
    onCancelSelection: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("경로 선택") },
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
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                if (uiState.selectedRouteIndex != null) {
                    // 경로 미리보기
                    RoutePreview(
                        route = uiState.routes[uiState.selectedRouteIndex],
                        onConfirm = onConfirmRoute,
                        onCancel = onCancelSelection,
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    // 경로 목록
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(uiState.routes.size) { index ->
                            RouteCard(
                                route = uiState.routes[index],
                                onClick = { onRouteClick(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: Route,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 상단: 시간, 거리, 요금
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 소요 시간
                Text(
                    text = "${route.totalTime}분",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 거리 및 요금
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1fkm".format(route.totalDistance / 1000.0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${route.totalFare}원",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 환승 정보
            if (route.transferCount > 0) {
                Text(
                    text = "환승 ${route.transferCount}회",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 경로 상세 (아이콘 및 노선명)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                route.subPaths.forEach { subPath ->
                    when (subPath.trafficType) {
                        TrafficType.SUBWAY -> {
                            SubPathChip(
                                icon = Icons.Default.DirectionsSubway,
                                label = subPath.lane?.name ?: "지하철",
                                color = Color(0xFF0052A4)
                            )
                        }
                        TrafficType.BUS -> {
                            SubPathChip(
                                icon = Icons.Default.DirectionsBus,
                                label = subPath.lane?.busNo ?: "버스",
                                color = Color(0xFF53B332)
                            )
                        }
                        TrafficType.WALK -> {
                            if (subPath.distance > 100) { // 100m 이상 도보만 표시
                                SubPathChip(
                                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    label = "도보",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubPathChip(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RoutePreview(
    route: Route,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 경로의 중심 좌표 계산
    val allPoints = mutableListOf<LatLng>()
    route.subPaths.forEach { subPath ->
        val startLat = subPath.startLatitude
        val startLng = subPath.startLongitude
        val endLat = subPath.endLatitude
        val endLng = subPath.endLongitude

        if (startLat != null && startLng != null) {
            allPoints.add(LatLng(startLat, startLng))
        }
        subPath.passStations?.forEach { station ->
            allPoints.add(LatLng(station.latitude, station.longitude))
        }
        if (endLat != null && endLng != null) {
            allPoints.add(LatLng(endLat, endLng))
        }
    }

    val centerPosition = if (allPoints.isNotEmpty()) {
        val avgLat = allPoints.map { it.latitude }.average()
        val avgLng = allPoints.map { it.longitude }.average()
        LatLng(avgLat, avgLng)
    } else {
        LatLng(37.5665, 126.9780) // 기본값: 서울
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerPosition, 13f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 지도
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // 경로 그리기
            route.subPaths.forEachIndexed { index, subPath ->
                val points = mutableListOf<LatLng>()
                val graphicData = subPath.graphicData

                // 그래픽 데이터가 있으면 사용 (실제 경로)
                if (!graphicData.isNullOrEmpty()) {
                    graphicData.forEach { coord ->
                        points.add(LatLng(coord.latitude, coord.longitude))
                    }
                } else {
                    // 그래픽 데이터가 없으면 기존 방식 사용
                    var startLat = subPath.startLatitude
                    var startLng = subPath.startLongitude
                    var endLat = subPath.endLatitude
                    var endLng = subPath.endLongitude

                    // 좌표가 null인 경우 (주로 도보 구간) 이전/다음 구간의 좌표 사용
                    if (startLat == null || startLng == null) {
                        // 이전 구간의 끝점을 시작점으로
                        val prevSubPath = route.subPaths.getOrNull(index - 1)
                        startLat = prevSubPath?.endLatitude
                        startLng = prevSubPath?.endLongitude
                    }
                    if (endLat == null || endLng == null) {
                        // 다음 구간의 시작점을 끝점으로
                        val nextSubPath = route.subPaths.getOrNull(index + 1)
                        endLat = nextSubPath?.startLatitude
                        endLng = nextSubPath?.startLongitude
                    }

                    if (startLat != null && startLng != null && endLat != null && endLng != null) {
                        points.add(LatLng(startLat, startLng))

                        subPath.passStations?.forEach { station ->
                            points.add(LatLng(station.latitude, station.longitude))
                        }

                        points.add(LatLng(endLat, endLng))
                    }
                }

                // 포인트가 있으면 Polyline 그리기
                if (points.isNotEmpty()) {
                    val lineColor = when (subPath.trafficType) {
                        TrafficType.SUBWAY -> Color(0xFF0052A4)
                        TrafficType.BUS -> Color(0xFF53B332)
                        TrafficType.WALK -> Color.Gray
                    }

                    Polyline(
                        points = points,
                        color = lineColor,
                        width = 10f
                    )
                }
            }

            // 시작점과 종점 마커
            if (allPoints.isNotEmpty()) {
                Marker(
                    state = rememberMarkerState(position = allPoints.first()),
                    title = "출발"
                )
                Marker(
                    state = rememberMarkerState(position = allPoints.last()),
                    title = "도착"
                )
            }
        }

        // 상단 경로 정보 카드
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${route.totalTime}분",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.1fkm".format(route.totalDistance / 1000.0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${route.totalFare}원",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (route.transferCount > 0) {
                    Text(
                        text = "환승 ${route.transferCount}회",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 하단 버튼들
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "다른 경로 선택")
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "이 경로 사용")
            }
        }
    }
}
