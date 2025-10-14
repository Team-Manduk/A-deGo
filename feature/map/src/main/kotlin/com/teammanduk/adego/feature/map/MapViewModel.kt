package com.teammanduk.adego.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.usecase.JoinRoomUseCase
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
    private val locationRepository: com.teammanduk.adego.core.domain.repository.LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    init {
        val roomId = savedStateHandle.get<String>("roomId") ?: ""
        val userId = savedStateHandle.get<String>("userId") ?: ""

        userRepository.setCurrentUser(userId)

        viewModelScope.launch {
            joinRoom(roomId)
                .catch { emit(null to emptyList()) }
                .collect { (room, participants) ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            room = room?.toUiModel(),
                            participants = participants.toUiModels()
                        )
                    }
                }
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
                    .collect {
                        _uiState.update { currentState ->
                            currentState.copy(
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
        }
    }

    private fun reduce(state: MapUiState, intent: MapIntent): MapUiState {
        return when (intent) {
            is MapIntent.SelectParticipant -> state.copy(selectedParticipantIndex = intent.index)
            MapIntent.ClearError -> state.copy(error = null)
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
