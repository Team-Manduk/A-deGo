package com.teammanduk.adego.feature.place.util

import com.google.android.gms.maps.model.LatLng
import com.teammanduk.adego.core.common.constants.PlaceConstants
import javax.inject.Inject

/**
 * 위치 관련 검증 로직을 담당하는 유틸리티 클래스
 */
class LocationValidator @Inject constructor() {

    /**
     * 두 위치가 실질적으로 같은지 확인
     * @param pos1 첫 번째 위치
     * @param pos2 두 번째 위치
     * @return 두 위치가 허용 오차 범위 내에서 같으면 true (약 0.1m 오차)
     */
    fun isSameLocation(pos1: LatLng, pos2: LatLng): Boolean {
        val latDiff = kotlin.math.abs(pos1.latitude - pos2.latitude)
        val lngDiff = kotlin.math.abs(pos1.longitude - pos2.longitude)
        return latDiff < PlaceConstants.LOCATION_COMPARISON_PRECISION &&
               lngDiff < PlaceConstants.LOCATION_COMPARISON_PRECISION
    }
}
