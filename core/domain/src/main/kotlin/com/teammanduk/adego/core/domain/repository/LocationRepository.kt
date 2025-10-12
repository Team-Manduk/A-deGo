package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /**
     * 현재 위치 가져오기 (1회)
     */
    suspend fun getCurrentLocation(): Result<ParticipantLocation>

    /**
     * 위치 업데이트 구독 (실시간)
     */
    fun observeLocationUpdates(): Flow<ParticipantLocation>

    /**
     * 위치 추적 시작
     */
    suspend fun startLocationTracking(): Result<Unit>

    /**
     * 위치 추적 중지
     */
    suspend fun stopLocationTracking(): Result<Unit>
}
