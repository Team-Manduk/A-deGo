package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow

/**
 * 위치 추적 및 관리 Repository
 *
 * TODO: 백그라운드 위치 추적 기능 추가 필요
 *
 * 현재 상태:
 * - MapScreen이 active일 때만 위치 추적이 동작함
 * - 화면 벗어나거나 앱 종료 시 위치 추적 중단
 *
 * 추후 구현 필요 사항:
 * 1. BackgroundLocationService (Foreground Service)
 *    - 세션이 활성화되면 자동으로 서비스 시작
 *    - Notification으로 "위치 공유 중" 표시
 *    - 앱이 백그라운드에 있어도 위치 추적 계속
 *
 * 2. 배터리 최적화
 *    - 위치 업데이트 간격 조절 (예: 10초 → 30초)
 *    - 정확도 요구사항 완화
 *    - 절전 모드 고려
 *
 * 3. 추가 메서드 (예시)
 *    - suspend fun startBackgroundTracking()
 *    - suspend fun stopBackgroundTracking()
 *    - fun isBackgroundTrackingActive(): Boolean
 */
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
     * 위치 추적 시작 (Foreground only)
     *
     * 현재는 MapScreen이 보이는 동안만 동작
     * TODO: 백그라운드 추적은 별도 서비스로 구현 필요
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
