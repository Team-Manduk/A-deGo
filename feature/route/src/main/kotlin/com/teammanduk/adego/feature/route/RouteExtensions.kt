package com.teammanduk.adego.feature.route

import com.google.android.gms.maps.model.LatLng
import com.teammanduk.adego.core.model.Route

internal fun Route.calculateAllPoints(): List<LatLng> {
    val points = mutableListOf<LatLng>()
    subPaths.forEach { subPath ->
        subPath.startLatitude?.let { lat ->
            subPath.startLongitude?.let { lng ->
                points.add(LatLng(lat, lng))
            }
        }
        subPath.passStations?.forEach { station ->
            points.add(LatLng(station.latitude, station.longitude))
        }
        subPath.endLatitude?.let { lat ->
            subPath.endLongitude?.let { lng ->
                points.add(LatLng(lat, lng))
            }
        }
    }
    return points
}

internal fun List<LatLng>.center(): LatLng {
    return if (isNotEmpty()) {
        val avgLat = map { it.latitude }.average()
        val avgLng = map { it.longitude }.average()
        LatLng(avgLat, avgLng)
    } else {
        LatLng(37.5665, 126.9780)
    }
}
