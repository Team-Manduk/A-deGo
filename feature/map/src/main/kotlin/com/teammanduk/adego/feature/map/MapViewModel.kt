package com.teammanduk.adego.feature.map

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.JoinRoomUseCase
import com.teammanduk.adego.core.domain.usecase.SearchRouteUseCase
import com.teammanduk.adego.core.domain.usecase.TrackAndUpdateLocationUseCase
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapUiState
import com.teammanduk.adego.feature.map.model.toUiModel
import com.teammanduk.adego.feature.map.model.toUiModels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val joinRoom: JoinRoomUseCase,
    private val trackAndUpdateLocation: TrackAndUpdateLocationUseCase,
    private val userRepository: com.teammanduk.adego.core.domain.repository.UserRepository,
    private val roomRepository: com.teammanduk.adego.core.domain.repository.RoomRepository,
    private val locationRepository: com.teammanduk.adego.core.domain.repository.LocationRepository,
    private val searchRouteUseCase: com.teammanduk.adego.core.domain.usecase.SearchRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val roomId: String = savedStateHandle.get<String>("roomId") ?: ""
    val userId: String = savedStateHandle.get<String>("userId") ?: ""

    init {
        _uiState.update { it.copy(roomId = roomId, userId = userId) }
        userRepository.setCurrentUser(userId)
    }

    fun setSelectedRoute(route: com.teammanduk.adego.core.model.Route) {
        _uiState.update {
            it.copy(
                searchedRoutes = listOf(route),
                selectedRouteIndex = 0
            )
        }
    }

    fun startLocationTracking() {
        viewModelScope.launch {
            try {
                trackAndUpdateLocation()
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                error = "위치 추적 실패: ${e.message}",
                                isLocationTrackingActive = false
                            )
                        }
                    }
                    .collect { location ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                myLocation = location,
                                isLocationTrackingActive = true,
                                isInitialLocationLoaded = if (!currentState.isInitialLocationLoaded) {
                                    true
                                } else {
                                    currentState.isInitialLocationLoaded
                                }
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "위치 추적 시작 실패: ${e.message}") }
            }
        }
    }

    fun onAction(intent: MapIntent) {
        when (intent) {
            is MapIntent.SelectParticipant -> {
                _uiState.update { reduce(it, intent) }
            }

            MapIntent.ClearError -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.ShowInviteDialog -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.DismissInviteDialog -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.ShowRouteDialog -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.DismissRouteDialog -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.ShowSelectStartPlace -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.DismissSelectStartPlace -> {
                _uiState.update { reduce(it, intent) }
            }
            is MapIntent.StartPlaceSelected -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.ShowSearchPlace -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.DismissSearchPlace -> {
                _uiState.update { reduce(it, intent) }
            }
            MapIntent.SearchRoute -> {
                searchRoute()
            }
            is MapIntent.SelectRoute -> {
                _uiState.update { reduce(it, intent) }
            }
        }
    }

    private fun reduce(state: MapUiState, intent: MapIntent): MapUiState {
        return when (intent) {
            is MapIntent.SelectParticipant -> state.copy(selectedParticipantIndex = intent.index)
            MapIntent.ClearError -> state.copy(error = null)
            MapIntent.ShowInviteDialog -> state.copy(showInviteDialog = true)
            MapIntent.DismissInviteDialog -> state.copy(showInviteDialog = false)
            MapIntent.ShowRouteDialog -> state.copy(showRouteDialog = true)
            MapIntent.DismissRouteDialog -> state.copy(
                showRouteDialog = false,
                searchedRoutes = emptyList(),
                selectedRouteIndex = null
            )
            MapIntent.ShowSelectStartPlace -> state.copy(
                showSelectStartPlace = true,
                showRouteDialog = false
            )
            MapIntent.DismissSelectStartPlace -> state.copy(
                showSelectStartPlace = false,
                showRouteDialog = true
            )
            is MapIntent.StartPlaceSelected -> state.copy(
                startPlace = intent.place,
                showSelectStartPlace = false,
                showRouteDialog = true
            )
            MapIntent.ShowSearchPlace -> state.copy(showSearchPlace = true)
            MapIntent.DismissSearchPlace -> state.copy(showSearchPlace = false)
            MapIntent.SearchRoute -> state
            is MapIntent.SelectRoute -> state.copy(selectedRouteIndex = intent.routeIndex)
        }
    }

    private fun searchRoute() {
        val currentState = _uiState.value
        val startPlace = currentState.startPlace
        val destination = currentState.room?.destination

        if (startPlace == null || destination == null) {
            _uiState.update { it.copy(error = "출발지와 목적지를 모두 선택해주세요.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSearchingRoute = true, error = null) }

                val routes = searchRouteUseCase(
                    startLat = startPlace.latitude,
                    startLng = startPlace.longitude,
                    endLat = destination.latitude,
                    endLng = destination.longitude
                )

                _uiState.update {
                    it.copy(
                        searchedRoutes = routes,
                        isSearchingRoute = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "경로 검색에 실패했습니다: ${e.message}",
                        isSearchingRoute = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationRepository.stopLocationTracking()
            roomRepository.clearCurrentRoom()
            userRepository.clearCurrentUser()
        }
    }
}
