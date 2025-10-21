package com.teammanduk.adego.feature.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType

@Composable
internal fun RoutePreviewScreen(
    route: Route,
    isLoadingDetails: Boolean,
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
        position = CameraPosition.fromLatLngZoom(centerPosition, 14f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 지도 (상단 40%)
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f),
                cameraPositionState = cameraPositionState
            ) {
                // 경로 그리기
                route.subPaths.forEachIndexed { index, subPath ->
                    val points = mutableListOf<LatLng>()
                    val graphicData = subPath.graphicData

                    if (!graphicData.isNullOrEmpty()) {
                        graphicData.forEach { coord ->
                            points.add(LatLng(coord.latitude, coord.longitude))
                        }
                    } else {
                        var startLat = subPath.startLatitude
                        var startLng = subPath.startLongitude
                        var endLat = subPath.endLatitude
                        var endLng = subPath.endLongitude

                        if (startLat == null || startLng == null) {
                            val prevSubPath = route.subPaths.getOrNull(index - 1)
                            startLat = prevSubPath?.endLatitude
                            startLng = prevSubPath?.endLongitude
                        }
                        if (endLat == null || endLng == null) {
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

                    if (points.isNotEmpty()) {
                        val lineColor = when (subPath.trafficType) {
                            TrafficType.SUBWAY -> Color(0xFF0052A4)
                            TrafficType.BUS -> Color(0xFF00C73C)
                            TrafficType.WALK -> Color(0xFF999999)
                        }

                        Polyline(
                            points = points,
                            color = lineColor,
                            width = 12f
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

            // 상단 경로 요약 칩
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(route.totalTime),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF000000)
                        )
                        Text(
                            text = "│",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE0E0E0)
                        )
                        // 노선 번호들
                        route.subPaths.filter { it.trafficType != TrafficType.WALK }.forEach { subPath ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (subPath.trafficType == TrafficType.SUBWAY) {
                                    Color(0xFF0052A4)
                                } else {
                                    Color(0xFF00C73C)
                                }
                            ) {
                                Text(
                                    text = subPath.lane?.name ?: subPath.lane?.busNo ?: "",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "${String.format("%,d", route.totalFare)}원",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF000000)
                    )
                }
            }
        }

        // 하단 스크롤 가능한 경로 상세 정보 (60%)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter)
                .background(Color.White, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 시간 정보
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(route.totalTime),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF000000)
                    )
                    Text(
                        text = "오후 도착",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }

            // 상세 타임라인
            items(route.subPaths.size) { index ->
                val subPath = route.subPaths[index]

                when (subPath.trafficType) {
                    TrafficType.WALK -> {
                        // 도보 구간
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 타임라인 라인
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(60.dp)
                                    .background(Color(0xFFE0E0E0))
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                        contentDescription = "도보",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "도보 ${(subPath.distance).toInt()}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF000000)
                                    )
                                    Text(
                                        text = formatTime(subPath.sectionTime),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }
                        }
                    }
                    TrafficType.SUBWAY, TrafficType.BUS -> {
                        // 대중교통 구간
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 승차 정보
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // 타임라인 원형 마커
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = if (subPath.trafficType == TrafficType.SUBWAY) {
                                                Color(0xFF0052A4)
                                            } else {
                                                Color(0xFF00C73C)
                                            },
                                            shape = RoundedCornerShape(50)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White, shape = RoundedCornerShape(50))
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subPath.startName ?: "출발지",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF000000)
                                    )
                                    Text(
                                        text = "15:10",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }

                            // 노선 정보 카드
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF8F9FA)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (subPath.trafficType == TrafficType.SUBWAY) {
                                                Color(0xFF0052A4)
                                            } else {
                                                Color(0xFF00C73C)
                                            }
                                        ) {
                                            Text(
                                                text = subPath.lane?.name ?: subPath.lane?.busNo ?: "",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        subPath.stationCount?.let { count ->
                                            Text(
                                                text = "${count}개 정류장",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF666666)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${formatTime(subPath.sectionTime)} 정류장 이동",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }

                            // 하차 정보
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFFFF5252), shape = RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "하차",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                }

                                Text(
                                    text = subPath.endName ?: "도착지",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF000000)
                                )
                            }
                        }
                    }
                }
            }

            // 하단 버튼들
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00C73C)
                        )
                    ) {
                        Text(
                            text = "다른 경로 선택",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C73C)
                        ),
                        enabled = !isLoadingDetails
                    ) {
                        Text(
                            text = "경로 확정",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 로딩 인디케이터
        if (isLoadingDetails) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00C73C))
            }
        }
    }
}

// 프리뷰용 샘플 데이터
private fun getSampleRouteForPreview(): Route {
    return Route(
        totalTime = 43,
        totalDistance = 15000,
        totalFare = 1500,
        transferCount = 1,
        pathType = 3,
        subPaths = listOf(
            SubPath(
                trafficType = TrafficType.WALK,
                distance = 150.0,
                sectionTime = 2,
                startLatitude = 35.1795,
                startLongitude = 129.0756,
                endLatitude = 35.1800,
                endLongitude = 129.0760
            ),
            SubPath(
                trafficType = TrafficType.SUBWAY,
                distance = 8000.0,
                sectionTime = 25,
                startName = "부산사상터미널역",
                endName = "덕천역",
                stationCount = 10,
                lane = Lane(
                    name = "2호선",
                    subwayCode = 2
                ),
                startLatitude = 35.1800,
                startLongitude = 129.0760,
                endLatitude = 35.2050,
                endLongitude = 129.0850
            ),
            SubPath(
                trafficType = TrafficType.WALK,
                distance = 200.0,
                sectionTime = 3,
                startLatitude = 35.2050,
                startLongitude = 129.0850,
                endLatitude = 35.2055,
                endLongitude = 129.0855
            ),
            SubPath(
                trafficType = TrafficType.BUS,
                distance = 5000.0,
                sectionTime = 15,
                startName = "덕천역",
                endName = "만덕3동주민센터",
                stationCount = 8,
                lane = Lane(
                    name = "133번",
                    busNo = "133",
                    type = 12
                ),
                startLatitude = 35.2055,
                startLongitude = 129.0855,
                endLatitude = 35.2200,
                endLongitude = 129.0900
            ),
            SubPath(
                trafficType = TrafficType.WALK,
                distance = 100.0,
                sectionTime = 1,
                startLatitude = 35.2200,
                startLongitude = 129.0900,
                endLatitude = 35.2205,
                endLongitude = 129.0905
            )
        )
    )
}

// 프리뷰
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RoutePreviewScreenPreview() {
    MaterialTheme {
        RoutePreviewScreen(
            route = getSampleRouteForPreview(),
            isLoadingDetails = false,
            onConfirm = {},
            onCancel = {}
        )
    }
}
