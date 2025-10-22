package com.teammanduk.adego.feature.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
import com.teammanduk.adego.core.ui.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutePreviewScreen(
    route: Route,
    isLoadingDetails: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
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
        // 지도 (전체 화면)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
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
                .padding(top = 16.dp)
                .systemBarsPadding(),
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

        // 바텀시트
        ModalBottomSheet(
            onDismissRequest = { /* 바텀시트는 닫히지 않도록 */ },
            sheetState = sheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 경로 요약 정보
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 제목
                        Text(
                            text = "경로 상세",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF000000)
                        )

                        // 요약 정보 카드
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // 총 소요시간
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "소요시간",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = formatTime(route.totalTime),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF000000)
                                    )
                                }

                                // 환승 횟수
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "환승",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "${route.transferCount}회",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF000000)
                                    )
                                }

                                // 총 거리
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "거리",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "%.1fkm".format(route.totalDistance / 1000.0),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF000000)
                                    )
                                }

                                // 요금
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "요금",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "${String.format("%,d", route.totalFare)}원",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF000000)
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFFE0E0E0))

                        Text(
                            text = "이동 경로",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF000000)
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 도보 아이콘
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = "도보",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF666666)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "도보 이동",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF000000)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${(subPath.distance).toInt()}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF999999)
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF999999)
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 노선 정보 헤더
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 교통수단 아이콘
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (subPath.trafficType == TrafficType.SUBWAY) {
                                                Color(0xFF0052A4)
                                            } else {
                                                Color(0xFF00C73C)
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (subPath.trafficType == TrafficType.SUBWAY) {
                                                        Icons.Default.DirectionsSubway
                                                    } else {
                                                        Icons.Default.DirectionsBus
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = Color.White
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            // 노선명
                                            Text(
                                                text = subPath.lane?.name ?: subPath.lane?.busNo ?:
                                                    if (subPath.trafficType == TrafficType.SUBWAY) "지하철" else "버스",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (subPath.trafficType == TrafficType.SUBWAY) {
                                                    Color(0xFF0052A4)
                                                } else {
                                                    Color(0xFF00C73C)
                                                }
                                            )

                                            // 소요시간 및 정류장 수
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = formatTime(subPath.sectionTime),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF666666)
                                                )
                                                subPath.stationCount?.let { count ->
                                                    Text(
                                                        text = "•",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF999999)
                                                    )
                                                    Text(
                                                        text = "${count}개 정류장",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF666666)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Divider(color = Color(0xFFE0E0E0))

                                    // 승차역 → 하차역
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 승차
                                        Column {
                                            Text(
                                                text = "승차",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF999999)
                                            )
                                            Text(
                                                text = subPath.startName ?: "출발지",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF000000)
                                            )
                                        }

                                        // 화살표
                                        Text(
                                            text = "→",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color(0xFFCCCCCC)
                                        )

                                        // 하차
                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "하차",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF999999)
                                            )
                                            Text(
                                                text = subPath.endName ?: "도착지",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF000000)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 하단 버튼들
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6200EE)
                            )
                        ) {
                            Text(
                                text = "다른 경로",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EE)
                            ),
                            enabled = !isLoadingDetails
                        ) {
                            if (isLoadingDetails) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "이 경로로 출발",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 프리뷰용 샘플 데이터
private fun getSampleRouteForPreview(): Route {
    return Route(
        totalTime = 43 * 60, // 43분 → 2580초
        totalDistance = 15000,
        totalFare = 1500,
        transferCount = 1,
        pathType = 3,
        subPaths = listOf(
            SubPath(
                trafficType = TrafficType.WALK,
                distance = 150.0,
                sectionTime = 2 * 60, // 2분 → 120초
                startLatitude = 35.1795,
                startLongitude = 129.0756,
                endLatitude = 35.1800,
                endLongitude = 129.0760
            ),
            SubPath(
                trafficType = TrafficType.SUBWAY,
                distance = 8000.0,
                sectionTime = 25 * 60, // 25분 → 1500초
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
                sectionTime = 3 * 60, // 3분 → 180초
                startLatitude = 35.2050,
                startLongitude = 129.0850,
                endLatitude = 35.2055,
                endLongitude = 129.0855
            ),
            SubPath(
                trafficType = TrafficType.BUS,
                distance = 5000.0,
                sectionTime = 15 * 60, // 15분 → 900초
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
                sectionTime = 1 * 60, // 1분 → 60초
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
