package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.PlaceRepository
import javax.inject.Inject

class ClearSelectedPlaceUseCase @Inject constructor(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke() {
        placeRepository.clearSelectedPlace()
    }
}
