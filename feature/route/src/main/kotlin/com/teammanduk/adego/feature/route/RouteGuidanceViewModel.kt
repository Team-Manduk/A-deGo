package com.teammanduk.adego.feature.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.GetSelectedRouteUseCase
import com.teammanduk.adego.core.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteGuidanceUiState(
    val isLoading: Boolean = true,
    val route: Route? = null,
    val error: String? = null
)

@HiltViewModel
class RouteGuidanceViewModel @Inject constructor(
    private val getSelectedRouteUseCase: GetSelectedRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteGuidanceUiState())
    val uiState: StateFlow<RouteGuidanceUiState> = _uiState.asStateFlow()

    init {
        loadSelectedRoute()
    }

    private fun loadSelectedRoute() {
        viewModelScope.launch {
            _uiState.value = RouteGuidanceUiState(isLoading = true)
            try {
                Log.d("RouteGuidanceViewModel", "Loading selected route from session")
                val result = getSelectedRouteUseCase()

                result.onSuccess { route ->
                    if (route != null) {
                        Log.d("RouteGuidanceViewModel", "Route loaded successfully")
                        _uiState.value = RouteGuidanceUiState(
                            isLoading = false,
                            route = route
                        )
                    } else {
                        Log.w("RouteGuidanceViewModel", "No route found")
                        _uiState.value = RouteGuidanceUiState(
                            isLoading = false,
                            error = "선택된 경로가 없습니다"
                        )
                    }
                }.onFailure { error ->
                    Log.e("RouteGuidanceViewModel", "Failed to load route", error)
                    _uiState.value = RouteGuidanceUiState(
                        isLoading = false,
                        error = "경로를 불러오는 중 오류가 발생했습니다: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("RouteGuidanceViewModel", "Exception while loading route", e)
                _uiState.value = RouteGuidanceUiState(
                    isLoading = false,
                    error = "경로를 불러오는 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }
}
