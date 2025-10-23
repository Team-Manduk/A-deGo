package com.teammanduk.adego.feature.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        var isFirstGraphicData = true // 첫 번째 graphicData 구간 확인용

        route.subPaths.forEachIndexed { index, subPath ->
            val points = mutableListOf<LatLng>()
            val graphicData = subPath.graphicData

            var startLat = subPath.startLatitude
            var startLng = subPath.startLongitude
            var endLat = subPath.endLatitude
            var endLng = subPath.endLongitude

            // 시작/종료 좌표 보정
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

            if (!graphicData.isNullOrEmpty()) {
                val firstCoord = graphicData.first()
                val lastCoord = graphicData.last()

                // 이전 구간과의 연결
                previousEndPoint?.let { prevPoint ->
                    val distance = Math.sqrt(
                        Math.pow(prevPoint.latitude - firstCoord.latitude, 2.0) +
                                Math.pow(prevPoint.longitude - firstCoord.longitude, 2.0)
                    ) * 111000

                    if (distance > 1) {
                        Polyline(
                            points = listOf(
                                prevPoint,
                                LatLng(firstCoord.latitude, firstCoord.longitude)
                            ),
                            color = Color(0xFFCCCCCC),
                            width = 8f,
                            pattern = dashedPattern
                        )
                    }
                }

                if (previousEndPoint == null && isFirstGraphicData) {
                    // Route 시작점과 첫 graphicData 연결
                    val routeStartLat = route.startLatitude
                    val routeStartLng = route.startLongitude

                    if (routeStartLat != null && routeStartLng != null) {
                        val distance = Math.sqrt(
                            Math.pow(routeStartLat - firstCoord.latitude, 2.0) +
                                    Math.pow(routeStartLng - firstCoord.longitude, 2.0)
                        ) * 111000

                        if (distance > 1) {
                            Polyline(
                                points = listOf(
                                    LatLng(routeStartLat, routeStartLng),
                                    LatLng(firstCoord.latitude, firstCoord.longitude)
                                ),
                                color = Color(0xFFCCCCCC),
                                width = 8f,
                                pattern = dashedPattern
                            )
                        }
                    }
                    isFirstGraphicData = false
                }

                graphicData.forEach { coord ->
                    points.add(LatLng(coord.latitude, coord.longitude))
                }

                previousEndPoint = LatLng(lastCoord.latitude, lastCoord.longitude)
            } else {
                // graphicData가 없는 경우 fallback
                if (startLat != null && startLng != null && endLat != null && endLng != null) {
                    previousEndPoint?.let { prevPoint ->
                        val distance = Math.sqrt(
                            Math.pow(prevPoint.latitude - startLat, 2.0) +
                                    Math.pow(prevPoint.longitude - startLng, 2.0)
                        ) * 111000

                        if (distance > 1) {
                            Polyline(
                                points = listOf(
                                    prevPoint,
                                    LatLng(startLat, startLng)
                                ),
                                color = Color(0xFFCCCCCC),
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
            }
        }

        // Route 도착지와의 연결
        val routeEndLat = route.endLatitude
        val routeEndLng = route.endLongitude

        if (routeEndLat != null && routeEndLng != null) {
            previousEndPoint?.let { prevPoint ->
                val distance = Math.sqrt(
                    Math.pow(prevPoint.latitude - routeEndLat, 2.0) +
                            Math.pow(prevPoint.longitude - routeEndLng, 2.0)
                ) * 111000

                if (distance > 1) {
                    Polyline(
                        points = listOf(
                            prevPoint,
                            LatLng(routeEndLat, routeEndLng)
                        ),
                        color = Color(0xFFCCCCCC),
                        width = 8f,
                        pattern = dashedPattern
                    )
                }
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
