package com.teammanduk.adego.core.domain.util

import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.domain.util.LocationUtils.calculateDistance

/**
 * Google Polyline 인코딩/디코딩 유틸리티
 *
 * Route의 graphicData를 Google Polyline 인코딩 형식으로 변환하거나
 * 인코딩된 polyline 문자열을 좌표 리스트로 디코딩
 */
object PolylineEncoder {
    /**
     * Route 객체를 polyline 문자열로 인코딩
     * RouteMapView의 실제 그리기 로직과 동일하게 연결 구간까지 포함
     */
    fun encode(route: Route): String {
        val coordinates = mutableListOf<Pair<Double, Double>>()
        var previousEndPoint: Pair<Double, Double>? = null
        var isFirstGraphicData = true

        route.subPaths.forEachIndexed { index, subPath ->
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

                if (previousEndPoint != null) {
                    if (calculateDistance(
                            previousEndPoint.first, previousEndPoint.second,
                            firstCoord.latitude, firstCoord.longitude
                        ) > 1) {
                        coordinates.add(previousEndPoint)
                    }
                } else if (isFirstGraphicData) {
                    val routeStartLat = route.startLatitude
                    val routeStartLng = route.startLongitude
                    if (routeStartLat != null && routeStartLng != null) {
                        if (calculateDistance(
                                routeStartLat, routeStartLng,
                                firstCoord.latitude, firstCoord.longitude
                            ) > 1) {
                            coordinates.add(routeStartLat to routeStartLng)
                        }
                    }
                    isFirstGraphicData = false
                }

                graphicData.forEach { coord ->
                    coordinates.add(coord.latitude to coord.longitude)
                }

                previousEndPoint = lastCoord.latitude to lastCoord.longitude
            } else {
                if (startLat != null && startLng != null && endLat != null && endLng != null) {
                    if (previousEndPoint != null) {
                        if (calculateDistance(
                                previousEndPoint.first, previousEndPoint.second,
                                startLat, startLng
                            ) > 1) {
                            coordinates.add(previousEndPoint)
                        }
                    }

                    coordinates.add(startLat to startLng)
                    subPath.passStations?.forEach { station ->
                        coordinates.add(station.latitude to station.longitude)
                    }
                    coordinates.add(endLat to endLng)

                    previousEndPoint = endLat to endLng
                }
            }
        }

        // Route 도착지 연결
        val routeEndLat = route.endLatitude
        val routeEndLng = route.endLongitude

        if (previousEndPoint != null && routeEndLat != null && routeEndLng != null) {
            if (calculateDistance(
                    previousEndPoint.first, previousEndPoint.second,
                    routeEndLat, routeEndLng
                ) > 1) {
                coordinates.add(routeEndLat to routeEndLng)
            }
        }

        return encodeCoordinates(coordinates)
    }

    private fun encodeCoordinates(coordinates: List<Pair<Double, Double>>): String {
        if (coordinates.isEmpty()) return ""

        val result = StringBuilder()

        coordinates.forEach { (lat, lng) ->
            val lat5 = (lat * 1e5).toInt()
            val lng5 = (lng * 1e5).toInt()

            encodeValue(lat5, result)
            encodeValue(lng5, result)
        }

        return result.toString()
    }

    private fun encodeValue(value: Int, result: StringBuilder) {
        var num = if (value < 0) ((-value) shl 1) or 1 else value shl 1

        while (num >= 0x20) {
            result.append(((0x20 or (num and 0x1f)) + 63).toChar())
            num = num shr 5
        }
        result.append((num + 63).toChar())
    }

    fun decode(encoded: String): List<Pair<Double, Double>> {
        if (encoded.isEmpty()) return emptyList()

        val coordinates = mutableListOf<Pair<Double, Double>>()
        var index = 0

        while (index < encoded.length) {
            var result = 1
            var shift = 0
            var b: Int

            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            val lat = if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 1
            shift = 0
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            val lng = if (result and 1 != 0) (result shr 1).inv() else result shr 1

            coordinates.add(lat / 1e5 to lng / 1e5)
        }

        return coordinates
    }
}
