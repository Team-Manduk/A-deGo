package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.LocationDataSource
import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow
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
     * 1. getLocationUpdates()가 lastLocation이 있으면 즉시 emit
     * 2. 없으면 requestLocationUpdates()로 새로운 위치 요청 시작
     * 3. 이후 실시간 업데이트 스트림 계속 수신
     */
    override fun observeLocationUpdates(): Flow<ParticipantLocation> {
        return locationDataSource.getLocationUpdates()
            .map { it.toModel() }
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

    override fun clearLocationState() {
        android.util.Log.d("MapPerformance", "[Repository] clearLocationState() 호출됨 at ${System.currentTimeMillis()}")
        lastUpdatedLocation = null
        lastUploadTimestamp = null
    }
}
