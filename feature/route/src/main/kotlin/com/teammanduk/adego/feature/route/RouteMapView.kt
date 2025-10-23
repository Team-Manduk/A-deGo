package com.teammanduk.adego.feature.route

import android.util.Log
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
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
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
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    showMyLocationButton: Boolean = false
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = com.google.maps.android.compose.MapProperties(
            isMyLocationEnabled = showMyLocationButton
        ),
        uiSettings = com.google.maps.android.compose.MapUiSettings(
            myLocationButtonEnabled = showMyLocationButton,
            zoomControlsEnabled = false
        )
    ) {
        var previousEndPoint: LatLng? = null // 이전 구간의 실제 마지막 좌표 추적
        val dashedPattern: List<PatternItem> = listOf(Dash(20f), Gap(10f)) // 점선 패턴

        route.subPaths.forEachIndexed { index, subPath ->
            val points = mutableListOf<LatLng>()
            val graphicData = subPath.graphicData

            // 시작/종료 좌표 가져오기
            var startLat = subPath.startLatitude
            var startLng = subPath.startLongitude
            var endLat = subPath.endLatitude
            var endLng = subPath.endLongitude

            Log.d("RouteMapView", "━━━━━ SubPath[$index] ${subPath.trafficType} ━━━━━")
            Log.d("RouteMapView", "원본 시작: ${subPath.startName} ($startLat, $startLng)")
            Log.d("RouteMapView", "원본 종료: ${subPath.endName} ($endLat, $endLng)")

            if (startLat == null || startLng == null) {
                val prevSubPath = route.subPaths.getOrNull(index - 1)
                startLat = prevSubPath?.endLatitude
                startLng = prevSubPath?.endLongitude
                Log.d("RouteMapView", "시작점 보정: 이전 구간 종료점 사용 ($startLat, $startLng)")
            }
            if (endLat == null || endLng == null) {
                val nextSubPath = route.subPaths.getOrNull(index + 1)
                endLat = nextSubPath?.startLatitude
                endLng = nextSubPath?.startLongitude
                Log.d("RouteMapView", "종료점 보정: 다음 구간 시작점 사용 ($endLat, $endLng)")
            }

            if (!graphicData.isNullOrEmpty()) {
                // graphicData가 있는 경우
                Log.d("RouteMapView", "SubPath[$index] graphicData: ${graphicData.size}개 좌표")

                val firstCoord = graphicData.first()
                val lastCoord = graphicData.last()

                Log.d("RouteMapView", "graphicData 첫 좌표: (${firstCoord.latitude}, ${firstCoord.longitude})")
                Log.d("RouteMapView", "graphicData 마지막 좌표: (${lastCoord.latitude}, ${lastCoord.longitude})")

                // 이전 구간과의 연결 확인 및 보정
                if (previousEndPoint != null) {
                    val distance = Math.sqrt(
                        Math.pow(previousEndPoint.latitude - firstCoord.latitude, 2.0) +
                        Math.pow(previousEndPoint.longitude - firstCoord.longitude, 2.0)
                    ) * 111000

                    Log.d("RouteMapView", "⚠️ 실제 좌표 간격: ${distance.toInt()}m")

                    if (distance > 1) { // 1m 이상 차이나면 점선으로 연결
                        Log.d("RouteMapView", "🔗 점선 연결: (${previousEndPoint.latitude}, ${previousEndPoint.longitude}) -> (${firstCoord.latitude}, ${firstCoord.longitude})")

                        // 점선 연결선 그리기
                        Polyline(
                            points = listOf(
                                previousEndPoint,
                                LatLng(firstCoord.latitude, firstCoord.longitude)
                            ),
                            color = Color(0xFFCCCCCC), // 연한 회색
                            width = 8f,
                            pattern = dashedPattern
                        )
                    }
                } else if (index == 0) {
                    // 첫 번째 구간: 원본 시작점과 graphicData 첫 좌표 비교
                    if (startLat != null && startLng != null) {
                        val distance = Math.sqrt(
                            Math.pow(startLat - firstCoord.latitude, 2.0) +
                            Math.pow(startLng - firstCoord.longitude, 2.0)
                        ) * 111000

                        if (distance > 1) {
                            Log.d("RouteMapView", "🏁 첫 구간 점선 연결: ($startLat, $startLng) -> (${firstCoord.latitude}, ${firstCoord.longitude})")

                            // 점선 연결선 그리기
                            Polyline(
                                points = listOf(
                                    LatLng(startLat, startLng),
                                    LatLng(firstCoord.latitude, firstCoord.longitude)
                                ),
                                color = Color(0xFFCCCCCC), // 연한 회색
                                width = 8f,
                                pattern = dashedPattern
                            )
                        }
                    }
                }

                // graphicData 추가
                graphicData.forEach { coord ->
                    points.add(LatLng(coord.latitude, coord.longitude))
                }

                // previousEndPoint 업데이트
                previousEndPoint = LatLng(lastCoord.latitude, lastCoord.longitude)
            } else {
                // graphicData가 없는 경우 (fallback)
                Log.d("RouteMapView", "SubPath[$index] graphicData 없음, 직선 경로 사용")

                if (startLat != null && startLng != null && endLat != null && endLng != null) {
                    // 이전 구간과 연결
                    if (previousEndPoint != null) {
                        val distance = Math.sqrt(
                            Math.pow(previousEndPoint.latitude - startLat, 2.0) +
                            Math.pow(previousEndPoint.longitude - startLng, 2.0)
                        ) * 111000

                        if (distance > 1) {
                            Log.d("RouteMapView", "🔗 점선 연결 (fallback)")

                            // 점선 연결선 그리기
                            Polyline(
                                points = listOf(
                                    previousEndPoint,
                                    LatLng(startLat, startLng)
                                ),
                                color = Color(0xFFCCCCCC), // 연한 회색
                                width = 8f,
                                pattern = dashedPattern
                            )
                        }
                    }

                    points.add(LatLng(startLat, startLng))
                    subPath.passStations?.forEach { station ->
                        points.add(LatLng(station.latitude, station.longitude))
                    }
                    points.add(LatLng(endLat, endLng))

                    previousEndPoint = LatLng(endLat, endLng)
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

                Log.d("RouteMapView", "Polyline 그리기: ${points.size}개 포인트, 색상=${lineColor}")
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
            Text(
                text = formatTime(route.totalTime),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF000000)
            )
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
