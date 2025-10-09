package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentSearchResultUseCase @Inject constructor(
    private val placeRepository: PlaceRepository
) {
    operator fun invoke(): Flow<Place?> {
        return placeRepository.getCurrentSearchResult()
    }
}
