package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(location: ParticipantLocation): Boolean {
        if (!shouldUpdate(location)) {
            return false
        }

        // 현재 사용자 ID를 내부에서 가져옴
        val userId = userRepository.getCurrentUserId()
        roomRepository.updateMyLocation(userId, location)

        // 업데이트 후 Repository에 상태 저장
        locationRepository.setLastUpdatedLocation(location)

        return true
    }

    private fun shouldUpdate(location: ParticipantLocation): Boolean {
        // Repository에서 마지막 업로드 정보 조회
        val lastLoc = locationRepository.getLastUpdatedLocation()
        val lastTime = locationRepository.getLastUploadTimestamp()

        // 첫 업데이트는 무조건 진행
        if (lastLoc == null || lastTime == null) {
            return true
        }

        val now = System.currentTimeMillis()
        val timeSinceLastUpdate = now - lastTime

        // 30초 이상 지났으면 무조건 업데이트 (살아있다는 신호)
        if (timeSinceLastUpdate >= 30_000L) {
            return true
        }

        // 거리 계산 (미터 단위)
        val distance = calculateDistance(
            lastLoc.latitude,
            lastLoc.longitude,
            location.latitude,
            location.longitude
        )

        // 25미터 이상 이동했을 때 업데이트
        return distance >= 25f
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val latDiff = (lat2 - lat1) * 111_320  // 위도 차이 (미터)
        val lonDiff = (lon2 - lon1) * 111_320 * kotlin.math.cos(Math.toRadians(lat1))  // 경도 차이 (미터)

        return kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff).toFloat()
    }
}
