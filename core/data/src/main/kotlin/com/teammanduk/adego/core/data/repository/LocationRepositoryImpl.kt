package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.LocationDataSource
import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDataSource: LocationDataSource
) : LocationRepository {

    // Firebase에 마지막으로 업로드된 위치 및 시각 저장
    private var lastUpdatedLocation: ParticipantLocation? = null
    private var lastUploadTimestamp: Long? = null

    override suspend fun getCurrentLocation(): Result<ParticipantLocation> {
        return try {
            val locationDto = locationDataSource.getCurrentLocation().getOrThrow()
            Result.success(locationDto.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 실시간 위치 업데이트를 Flow로 제공
     *
     * 동작:
     * 1. 첫 위치를 받을 때까지 재시도 (최대 10회, 2초 간격)
     * 2. 첫 위치 획득 후 FusedLocationProvider의 실시간 업데이트 스트림 시작
     *
     * 이렇게 하면 UI에서는 로딩 상태로 기다리다가
     * 첫 위치가 오면 바로 화면에 표시 가능
     */
    override fun observeLocationUpdates(): Flow<ParticipantLocation> {
        return flow {
            // 1. 첫 위치 획득 (재시도 포함)
            val firstLocation = getFirstLocationWithRetry()
            emit(firstLocation)

            // 2. 이후 실시간 업데이트 스트림
            locationDataSource.getLocationUpdates()
                .map { it.toModel() }
                .collect { location ->
                    emit(location)
                }
        }
    }

    /**
     * 첫 위치를 받을 때까지 재시도
     * - FusedLocationDataSource의 lastLocation이나 getCurrentLocation 중 빠른 것 사용
     * - 최대 10회 재시도, 각 시도마다 2초 대기
     */
    private suspend fun getFirstLocationWithRetry(): ParticipantLocation {
        repeat(10) { attempt ->
            getCurrentLocation().getOrNull()?.let { location ->
                return location
            }

            // 마지막 시도가 아니면 2초 대기
            if (attempt < 9) {
                delay(2000)
            }
        }

        throw Exception("위치를 가져올 수 없습니다. 위치 서비스를 확인해주세요.")
    }

    override suspend fun startLocationTracking(): Result<Unit> {
        return locationDataSource.startTracking()
    }

    override suspend fun stopLocationTracking(): Result<Unit> {
        return locationDataSource.stopTracking()
    }

    override fun getLastUpdatedLocation(): ParticipantLocation? {
        return lastUpdatedLocation
    }

    override fun setLastUpdatedLocation(location: ParticipantLocation) {
        lastUpdatedLocation = location
        lastUploadTimestamp = System.currentTimeMillis()
    }

    override fun getLastUploadTimestamp(): Long? {
        return lastUploadTimestamp
    }
}
