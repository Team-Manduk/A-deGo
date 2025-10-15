package com.teammanduk.adego.feature.route

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.navigation.Route as NavigationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectRouteUiState(
    val isLoading: Boolean = true,
    val routes: List<Route> = emptyList(),
    val error: String? = null,
    val selectedRouteIndex: Int? = null
)

@HiltViewModel
class SelectRouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectRouteUiState())
    val uiState: StateFlow<SelectRouteUiState> = _uiState.asStateFlow()

    init {
        val args = savedStateHandle.toRoute<NavigationRoute.SelectRoute>()
        searchRoutes(
            startLat = args.startLat,
            startLng = args.startLng,
            destLat = args.destLat,
            destLng = args.destLng
        )
    }

    fun selectRoute(index: Int) {
        _uiState.value = _uiState.value.copy(selectedRouteIndex = index)
    }

    fun deselectRoute() {
        _uiState.value = _uiState.value.copy(selectedRouteIndex = null)
    }

    private fun searchRoutes(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ) {
        viewModelScope.launch {
            _uiState.value = SelectRouteUiState(isLoading = true)
            try {
                val routes = routeRepository.searchRoute(
                    startLat = startLat,
                    startLng = startLng,
                    endLat = destLat,
                    endLng = destLng
                )
                _uiState.value = SelectRouteUiState(
                    isLoading = false,
                    routes = routes
                )
            } catch (e: Exception) {
                _uiState.value = SelectRouteUiState(
                    isLoading = false,
                    error = "경로를 검색하는 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }
}
