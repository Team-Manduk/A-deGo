package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import javax.inject.Inject

class SearchPlaceByTextUseCase @Inject constructor(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(query: String): List<Place> {
        return placeRepository.searchPlacesByText(query)
    }
}
