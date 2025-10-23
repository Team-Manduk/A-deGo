package com.teammanduk.adego.core.domain.util

import com.teammanduk.adego.core.model.Route

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

        route.subPaths.forEachIndexed { index, subPath ->
            val graphicData = subPath.graphicData

            // 시작/종료 좌표 가져오기
            var startLat = subPath.startLatitude
            var startLng = subPath.startLongitude
            var endLat = subPath.endLatitude
            var endLng = subPath.endLongitude

            // 시작점 보정
            if (startLat == null || startLng == null) {
                val prevSubPath = route.subPaths.getOrNull(index - 1)
                startLat = prevSubPath?.endLatitude
                startLng = prevSubPath?.endLongitude
            }
            // 종료점 보정
            if (endLat == null || endLng == null) {
                val nextSubPath = route.subPaths.getOrNull(index + 1)
                endLat = nextSubPath?.startLatitude
                endLng = nextSubPath?.startLongitude
            }

            if (!graphicData.isNullOrEmpty()) {
                // graphicData가 있는 경우
                val firstCoord = graphicData.first()
                val lastCoord = graphicData.last()

                // 이전 구간과의 연결 확인 및 보정
                if (previousEndPoint != null) {
                    val distance = Math.sqrt(
                        Math.pow(previousEndPoint.first - firstCoord.latitude, 2.0) +
                        Math.pow(previousEndPoint.second - firstCoord.longitude, 2.0)
                    ) * 111000

                    if (distance > 1) { // 1m 이상 차이나면 연결 포인트 추가
                        coordinates.add(previousEndPoint)
                    }
                } else if (index == 0) {
                    // 첫 번째 구간: 원본 시작점과 graphicData 첫 좌표 비교
                    if (startLat != null && startLng != null) {
                        val distance = Math.sqrt(
                            Math.pow(startLat - firstCoord.latitude, 2.0) +
                            Math.pow(startLng - firstCoord.longitude, 2.0)
                        ) * 111000

                        if (distance > 1) {
                            // 첫 구간 연결 포인트 추가
                            coordinates.add(startLat to startLng)
                        }
                    }
                }

                // graphicData 좌표들 추가
                graphicData.forEach { coord ->
                    coordinates.add(coord.latitude to coord.longitude)
                }

                // previousEndPoint 업데이트
                previousEndPoint = lastCoord.latitude to lastCoord.longitude
            } else {
                // graphicData가 없는 경우 (fallback)
                if (startLat != null && startLng != null && endLat != null && endLng != null) {
                    // 이전 구간과 연결
                    if (previousEndPoint != null) {
                        val distance = Math.sqrt(
                            Math.pow(previousEndPoint.first - startLat, 2.0) +
                            Math.pow(previousEndPoint.second - startLng, 2.0)
                        ) * 111000

                        if (distance > 1) {
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

        // 마지막 SubPath 이후 Route 도착지와의 연결 확인
        val routeEndLat = route.endLatitude
        val routeEndLng = route.endLongitude

        if (previousEndPoint != null && routeEndLat != null && routeEndLng != null) {
            val distance = Math.sqrt(
                Math.pow(previousEndPoint.first - routeEndLat, 2.0) +
                Math.pow(previousEndPoint.second - routeEndLng, 2.0)
            ) * 111000

            if (distance > 1) {
                // 마지막 graphicData 끝점과 실제 도착지 사이에 도착지 좌표 추가
                coordinates.add(routeEndLat to routeEndLng)
            }
        }

        return encodeCoordinates(coordinates)
    }

    /**
     * 좌표 리스트를 절대값으로 인코딩 (델타 방식 대신)
     */
    private fun encodeCoordinates(coordinates: List<Pair<Double, Double>>): String {
        if (coordinates.isEmpty()) return ""

        val result = StringBuilder()

        coordinates.forEach { (lat, lng) ->
            val lat5 = (lat * 1e5).toInt()
            val lng5 = (lng * 1e5).toInt()

            // 절대값 인코딩 (델타 없이)
            encodeValue(lat5, result)
            encodeValue(lng5, result)
        }

        return result.toString()
    }

    /**
     * 단일 값을 인코딩
     */
    private fun encodeValue(value: Int, result: StringBuilder) {
        var num = if (value < 0) ((-value) shl 1) or 1 else value shl 1

        while (num >= 0x20) {
            result.append(((0x20 or (num and 0x1f)) + 63).toChar())
            num = num shr 5
        }
        result.append((num + 63).toChar())
    }

    /**
     * polyline 문자열을 좌표 리스트로 디코딩 (절대값)
     *
     * @param encoded 인코딩된 polyline 문자열
     * @return 위도/경도 좌표 리스트
     */
    fun decode(encoded: String): List<Pair<Double, Double>> {
        if (encoded.isEmpty()) return emptyList()

        val coordinates = mutableListOf<Pair<Double, Double>>()
        var index = 0

        while (index < encoded.length) {
            var result = 1
            var shift = 0
            var b: Int

            // Latitude 디코딩 (절대값)
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            val lat = if (result and 1 != 0) (result shr 1).inv() else result shr 1

            // Longitude 디코딩 (절대값)
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
