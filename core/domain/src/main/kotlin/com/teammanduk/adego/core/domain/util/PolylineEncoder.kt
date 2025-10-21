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
     */
    fun encode(route: Route): String {
        val coordinates = mutableListOf<Pair<Double, Double>>()

        // 출발지 좌표 추가
        route.startLatitude?.let { lat ->
            route.startLongitude?.let { lng ->
                coordinates.add(lat to lng)
            }
        }

        // 각 SubPath의 graphicData 좌표 추가
        route.subPaths.forEach { subPath ->
            subPath.graphicData?.forEach { graphic ->
                coordinates.add(graphic.latitude to graphic.longitude)
            }
        }

        // 도착지 좌표 추가
        route.endLatitude?.let { lat ->
            route.endLongitude?.let { lng ->
                coordinates.add(lat to lng)
            }
        }

        return encodeCoordinates(coordinates)
    }

    /**
     * 좌표 리스트를 Google Polyline 인코딩 형식으로 변환
     *
     * Google Polyline Encoding Algorithm:
     * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
     */
    private fun encodeCoordinates(coordinates: List<Pair<Double, Double>>): String {
        if (coordinates.isEmpty()) return ""

        val result = StringBuilder()
        var prevLat = 0
        var prevLng = 0

        coordinates.forEach { (lat, lng) ->
            val lat5 = (lat * 1e5).toInt()
            val lng5 = (lng * 1e5).toInt()

            encodeValue(lat5 - prevLat, result)
            encodeValue(lng5 - prevLng, result)

            prevLat = lat5
            prevLng = lng5
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
     * polyline 문자열을 좌표 리스트로 디코딩
     *
     * @param encoded 인코딩된 polyline 문자열
     * @return 위도/경도 좌표 리스트
     */
    fun decode(encoded: String): List<Pair<Double, Double>> {
        if (encoded.isEmpty()) return emptyList()

        val coordinates = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var result = 1
            var shift = 0
            var b: Int

            // Latitude 디코딩
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            // Longitude 디코딩
            result = 1
            shift = 0
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            coordinates.add(lat / 1e5 to lng / 1e5)
        }

        return coordinates
    }
}
