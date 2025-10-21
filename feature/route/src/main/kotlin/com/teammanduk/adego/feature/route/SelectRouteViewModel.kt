package com.teammanduk.adego.feature.route

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.domain.usecase.SaveSelectedRouteUseCase
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
    val selectedRouteIndex: Int? = null,
    val isLoadingRouteDetails: Boolean = false
)

@HiltViewModel
class SelectRouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val saveSelectedRouteUseCase: SaveSelectedRouteUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectRouteUiState())
    val uiState: StateFlow<SelectRouteUiState> = _uiState.asStateFlow()

    // 출발지/도착지 좌표 저장
    private val startLat: Double
    private val startLng: Double
    private val destLat: Double
    private val destLng: Double

    init {
        val args = savedStateHandle.toRoute<NavigationRoute.SelectRoute>()
        startLat = args.startLat
        startLng = args.startLng
        destLat = args.destLat
        destLng = args.destLng
        searchRoutes(startLat, startLng, destLat, destLng)
    }

    fun selectRoute(index: Int) {
        viewModelScope.launch {
            val route = _uiState.value.routes.getOrNull(index) ?: return@launch

            // 이미 graphicData가 있으면 그냥 선택
            if (route.subPaths.any { it.graphicData != null }) {
                Log.d("SelectRouteViewModel", "경로[$index] - 이미 세부 정보 로드됨")
                _uiState.value = _uiState.value.copy(selectedRouteIndex = index)
                return@launch
            }

            // 세부 정보 로드
            Log.d("SelectRouteViewModel", "경로[$index] - 세부 정보 로드 시작")
            _uiState.value = _uiState.value.copy(
                selectedRouteIndex = index,
                isLoadingRouteDetails = true
            )

            try {
                val detailedRoute = routeRepository.getRouteDetails(
                    route = route,
                    startLat = startLat,
                    startLng = startLng,
                    endLat = destLat,
                    endLng = destLng
                )

                // 리스트에서 해당 경로 업데이트
                val updatedRoutes = _uiState.value.routes.toMutableList()
                updatedRoutes[index] = detailedRoute

                _uiState.value = _uiState.value.copy(
                    routes = updatedRoutes,
                    isLoadingRouteDetails = false
                )
                Log.d("SelectRouteViewModel", "경로[$index] - 세부 정보 로드 완료")
            } catch (e: Exception) {
                Log.e("SelectRouteViewModel", "경로[$index] - 세부 정보 로드 실패", e)
                _uiState.value = _uiState.value.copy(isLoadingRouteDetails = false)
            }
        }
    }

    fun deselectRoute() {
        _uiState.value = _uiState.value.copy(selectedRouteIndex = null)
    }

    /**
     * 선택한 경로를 로컬 DB에 저장
     */
    fun confirmAndSaveRoute() {
        viewModelScope.launch {
            val selectedIndex = _uiState.value.selectedRouteIndex ?: return@launch
            val selectedRoute = _uiState.value.routes.getOrNull(selectedIndex) ?: return@launch

            Log.d("SelectRouteViewModel", "선택한 경로를 로컬 DB에 저장 중...")
            val result = saveSelectedRouteUseCase(selectedRoute)

            result.onSuccess {
                Log.d("SelectRouteViewModel", "경로 저장 성공")
            }.onFailure { error ->
                Log.e("SelectRouteViewModel", "경로 저장 실패", error)
            }
        }
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
