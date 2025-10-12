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

    override suspend fun getCurrentLocation(): Result<ParticipantLocation> {
        return try {
            val locationDto = locationDataSource.getCurrentLocation().getOrThrow()
            Result.success(locationDto.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
}
