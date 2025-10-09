package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import javax.inject.Inject

class SearchPlaceByCoordinatesUseCase @Inject constructor(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Place? {
        return placeRepository.searchPlaceByCoordinates(latitude, longitude)
    }
}
