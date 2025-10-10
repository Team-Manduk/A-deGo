package com.teammanduk.adego.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.GetCurrentSearchResultUseCase
import com.teammanduk.adego.core.domain.usecase.GetSelectedPlaceUseCase
import com.teammanduk.adego.core.domain.usecase.SearchPlaceByCoordinatesUseCase
import com.teammanduk.adego.core.domain.usecase.SetSelectedPlaceUseCase
import com.teammanduk.adego.core.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectPlaceViewModel @Inject constructor(
    private val searchPlaceByCoordinatesUseCase: SearchPlaceByCoordinatesUseCase,
    private val getCurrentSearchResultUseCase: GetCurrentSearchResultUseCase,
    private val getSelectedPlaceUseCase: GetSelectedPlaceUseCase,
    private val setSelectedPlaceUseCase: SetSelectedPlaceUseCase
) : ViewModel() {

    // Repository의 검색 결과를 구독
    // SearchPlaceScreen에서 장소를 선택하면 여기에 반영됨
    val currentSearchResult: StateFlow<Place?> = getSelectedPlaceUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 위경도로 장소 검색 (지도 클릭 시)
    fun searchByCoordinates(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val place = searchPlaceByCoordinatesUseCase(latitude, longitude)
            // 검색 결과를 바로 선택된 장소로 설정
            place?.let { setSelectedPlaceUseCase(it) }
        }
    }

    // 검색 결과를 최종 선택으로 확정 (CreateScreen으로 전달)
    fun confirmSelection() {
        viewModelScope.launch {
            currentSearchResult.value?.let { place ->
                setSelectedPlaceUseCase(place)
            }
        }
    }
}
