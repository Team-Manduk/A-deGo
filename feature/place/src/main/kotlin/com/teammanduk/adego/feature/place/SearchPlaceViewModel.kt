package com.teammanduk.adego.feature.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.SearchPlaceByTextUseCase
import com.teammanduk.adego.core.domain.usecase.SetSelectedPlaceUseCase
import com.teammanduk.adego.core.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchPlaceViewModel @Inject constructor(
    private val searchPlaceByTextUseCase: SearchPlaceByTextUseCase,
    private val setSelectedPlaceUseCase: SetSelectedPlaceUseCase
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Place>>(emptyList())
    val searchResults: StateFlow<List<Place>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun searchPlaces(query: String) {
        // 이전 검색 취소
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            // 디바운싱: 사용자가 타이핑을 멈춘 후 300ms 후에 검색
            delay(300)

            _isLoading.value = true
            try {
                val results = searchPlaceByTextUseCase(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPlace(place: Place) {
        viewModelScope.launch {
            // 선택한 장소를 Repository에 저장
            setSelectedPlaceUseCase(place)
        }
    }

    fun clearResults() {
        _searchResults.value = emptyList()
    }
}
