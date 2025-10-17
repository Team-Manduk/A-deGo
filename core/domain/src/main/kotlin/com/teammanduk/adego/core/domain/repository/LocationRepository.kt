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

    /**
     * 마지막으로 업로드된 위치 조회
     * Firebase에 업로드 여부를 판단하기 위해 사용
     */
    fun getLastUpdatedLocation(): ParticipantLocation?

    /**
     * 업로드 완료된 위치 기록
     * Firebase 업로드 성공 후 호출
     */
    fun setLastUpdatedLocation(location: ParticipantLocation)

    /**
     * 마지막 업로드 시각 조회 (밀리초)
     * Firebase 업로드 시간 간격 판단에 사용
     */
    fun getLastUploadTimestamp(): Long?
}
