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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType
import com.teammanduk.adego.core.ui.util.formatTime

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
            // 서버에 경로 저장
            viewModel.confirmAndSaveRoute()

            // 선택한 경로 정보를 콜백으로 전달
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
                    RouteDetailScreen(
                        route = uiState.routes[uiState.selectedRouteIndex],
                        isLoadingDetails = uiState.isLoadingRouteDetails,
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
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 시간 + 환승 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 소요 시간 (크고 굵게)
                Text(
                    text = formatTime(route.totalTime),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )
            }

            // 요금 정보
            Text(
                text = "${String.format("%,d", route.totalFare)}원",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )

            // 경로 진행 바 (네이버 스타일)
            RouteProgressBar(subPaths = route.subPaths)

            // 경로 요약 정보 (대중교통 구간들을 간략하게 표시)
            val transitSubPaths = route.subPaths.filter { it.trafficType != TrafficType.WALK }
            if (transitSubPaths.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    transitSubPaths.forEachIndexed { index, transit ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 노선 번호 뱃지
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (transit.trafficType == TrafficType.SUBWAY) {
                                    Color(0xFF0052A4)
                                } else {
                                    Color(0xFF00C73C)
                                }
                            ) {
                                Text(
                                    text = transit.lane?.name ?: transit.lane?.busNo ?: "",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 정류장명
                            Text(
                                text = "${transit.startName ?: "출발지"} → ${transit.endName ?: "도착지"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF000000),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteProgressBar(subPaths: List<SubPath>) {
    // 거리가 0이거나 시간이 0인 도보 구간은 제외
    val filteredSubPaths = subPaths.filter { subPath ->
        subPath.trafficType != TrafficType.WALK || (subPath.distance > 0 && subPath.sectionTime > 0)
    }

    // 전체 시간 계산
    val totalTime = filteredSubPaths.sumOf { it.sectionTime }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 진행 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시작 아이콘
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "출발",
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF999999)
            )

            filteredSubPaths.forEach { subPath ->
                val weight = (subPath.sectionTime.toFloat() / totalTime.toFloat()).coerceAtLeast(0.1f)

                when (subPath.trafficType) {
                    TrafficType.WALK -> {
                        // 도보 구간 표시
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(20.dp)
                                .background(
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "도보",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF757575)
                            )
                        }
                    }
                    TrafficType.SUBWAY, TrafficType.BUS -> {
                        // 노선 번호 표시
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(20.dp)
                                .background(
                                    color = if (subPath.trafficType == TrafficType.SUBWAY) {
                                        Color(0xFF0052A4)
                                    } else {
                                        Color(0xFF00C73C)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subPath.lane?.name ?: subPath.lane?.busNo ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 종료 아이콘
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "도착",
                modifier = Modifier.size(18.dp),
                tint = Color(0xFFFF5252)
            )
        }

        // 구간별 소요시간 표시
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(modifier = Modifier.width(18.dp)) // 시작 아이콘 공간

            filteredSubPaths.forEach { subPath ->
                val weight = (subPath.sectionTime.toFloat() / totalTime.toFloat()).coerceAtLeast(0.1f)

                Box(
                    modifier = Modifier.weight(weight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(subPath.sectionTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (subPath.trafficType == TrafficType.WALK) {
                            Color(0xFF757575)
                        } else {
                            Color(0xFF666666)
                        },
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp)) // 종료 아이콘 공간
        }
    }
}

// 프리뷰용 샘플 데이터
private fun getSampleRoute(): Route {
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

private fun getSampleRoutes(): List<Route> {
    return listOf(
        getSampleRoute(),
        Route(
            totalTime = 48 * 60, // 48분 → 2880초
            totalDistance = 16500,
            totalFare = 1500,
            transferCount = 2,
            pathType = 3,
            subPaths = listOf(
                SubPath(
                    trafficType = TrafficType.SUBWAY,
                    distance = 9000.0,
                    sectionTime = 28 * 60, // 28분 → 1680초
                    startName = "부산사상터미널역",
                    endName = "화명역",
                    stationCount = 12,
                    lane = Lane(
                        name = "2호선",
                        subwayCode = 2
                    )
                ),
                SubPath(
                    trafficType = TrafficType.BUS,
                    distance = 6000.0,
                    sectionTime = 18 * 60, // 18분 → 1080초
                    startName = "화명역",
                    endName = "만덕3동주민센터",
                    stationCount = 9,
                    lane = Lane(
                        name = "161번",
                        busNo = "161"
                    )
                )
            )
        )
    )
}

// 프리뷰
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RouteCardPreview() {
    MaterialTheme {
        RouteCard(
            route = getSampleRoute(),
            onClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RouteProgressBarPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RouteProgressBar(subPaths = getSampleRoute().subPaths)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SelectRouteScreenPreview() {
    MaterialTheme {
        SelectRouteScreen(
            uiState = SelectRouteUiState(
                routes = getSampleRoutes(),
                isLoading = false,
                error = null
            ),
            onNavigateBack = {},
            onRouteClick = {},
            onConfirmRoute = {},
            onCancelSelection = {}
        )
    }
}

