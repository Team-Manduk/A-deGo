package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import javax.inject.Inject

class SetSelectedPlaceUseCase @Inject constructor(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(place: Place) {
        placeRepository.setSelectedPlace(place)
    }
}
