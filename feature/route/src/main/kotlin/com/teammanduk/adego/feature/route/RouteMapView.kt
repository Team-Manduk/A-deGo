package com.teammanduk.adego.feature.route

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.TrafficType
import com.teammanduk.adego.core.ui.util.formatTime

@Composable
internal fun RouteMapView(
    route: Route,
    allPoints: List<LatLng>,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
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
}

@Composable
internal fun BoxScope.RouteTopChips(route: Route) {
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
