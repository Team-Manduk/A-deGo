package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.ParticipantLocationDto
import kotlinx.coroutines.flow.Flow

interface LocationDataSource {
    /**
     * 현재 위치 가져오기
     */
    suspend fun getCurrentLocation(): Result<ParticipantLocationDto>

    /**
     * 위치 업데이트 Flow
     */
    fun getLocationUpdates(): Flow<ParticipantLocationDto>

    /**
     * 위치 추적 시작
     */
    suspend fun startTracking(): Result<Unit>

    /**
     * 위치 추적 중지
     */
    suspend fun stopTracking(): Result<Unit>
}
