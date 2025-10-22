package com.teammanduk.adego.feature.route

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teammanduk.adego.core.domain.usecase.GetSelectedRouteUseCase
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.navigation.Route as NavigationRoute
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
    private val getSelectedRouteUseCase: GetSelectedRouteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteGuidanceUiState())
    val uiState: StateFlow<RouteGuidanceUiState> = _uiState.asStateFlow()

    private val roomId: String
    private val userId: String

    init {
        val args = savedStateHandle.toRoute<NavigationRoute.RouteGuidance>()
        roomId = args.roomId
        userId = args.userId
        loadSelectedRoute()
    }

    private fun loadSelectedRoute() {
        viewModelScope.launch {
            _uiState.value = RouteGuidanceUiState(isLoading = true)
            try {
                Log.d("RouteGuidanceViewModel", "Loading route for roomId=$roomId, userId=$userId")
                val result = getSelectedRouteUseCase(roomId, userId)

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
