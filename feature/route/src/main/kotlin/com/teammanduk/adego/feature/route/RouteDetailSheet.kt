package com.teammanduk.adego.feature.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.TrafficType
import com.teammanduk.adego.core.ui.util.formatTime

@Composable
internal fun DragHandle() {
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

@Composable
internal fun RouteDetailContent(
    route: Route,
    isLoadingDetails: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 경로 요약 정보
        item {
            RouteSummary(route)
        }

        // 상세 타임라인
        items(route.subPaths.size) { index ->
            val subPath = route.subPaths[index]

            when (subPath.trafficType) {
                TrafficType.WALK -> {
                    WalkSection(subPath)
                }
                TrafficType.SUBWAY, TrafficType.BUS -> {
                    TransitSection(subPath)
                }
            }
        }

        // 하단 버튼들
        item {
            RouteActionButtons(
                isLoadingDetails = isLoadingDetails,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun RouteSummary(route: Route) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "경로 상세",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF000000)
        )

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
                SummaryItem("소요시간", formatTime(route.totalTime))
                SummaryItem("환승", "${route.transferCount}회")
                SummaryItem("거리", "%.1fkm".format(route.totalDistance / 1000.0))
                SummaryItem("요금", "${String.format("%,d", route.totalFare)}원")
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

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF000000)
        )
    }
}

@Composable
private fun WalkSection(subPath: com.teammanduk.adego.core.model.SubPath) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

@Composable
private fun TransitSection(subPath: com.teammanduk.adego.core.model.SubPath) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Text(
                        text = subPath.lane?.name ?: subPath.lane?.busNo
                        ?: if (subPath.trafficType == TrafficType.SUBWAY) "지하철" else "버스",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (subPath.trafficType == TrafficType.SUBWAY) {
                            Color(0xFF0052A4)
                        } else {
                            Color(0xFF00C73C)
                        }
                    )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Text(
                    text = "→",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFCCCCCC)
                )

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

@Composable
private fun RouteActionButtons(
    isLoadingDetails: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
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
