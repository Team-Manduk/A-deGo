package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class TrackAndUpdateLocationUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository
) {
    operator fun invoke(): Flow<ParticipantLocation> {
        return locationRepository.observeLocationUpdates()
            .onEach { location ->
                val userId = userRepository.getCurrentUserId()
                roomRepository.updateMyLocation(userId, location)
            }
    }
}
