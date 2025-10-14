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

/**
 * 지도 화면의 UI 상태
 */
data class MapUiState(
    val room: Room? = null,
    val participants: List<Participant> = emptyList(),
    val selectedParticipantIndex: Int = 0,
    val isLocationTrackingActive: Boolean = false,
    val isInitialLocationLoaded: Boolean = false,
    val error: String? = null,
    val showInviteDialog: Boolean = false,
    val showRouteDialog: Boolean = false
)

@HiltViewModel(assistedFactory = MapViewModel.Factory::class)
class MapViewModel @AssistedInject constructor(
    @Assisted("roomId") private val roomId: String,
    @Assisted("userId") val userId: String,  // public으로 변경
    private val roomRepository: RoomRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val roomId: String = savedStateHandle.get<String>("roomId") ?: ""
    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    init {
        userRepository.setCurrentUser(userId)
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

            is MapIntent.UpdateUserName -> {
                _uiState.update { reduce(it, intent) }
            }

            MapIntent.ConfirmUserName -> {
                confirmUserName()
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
        }
    }

    private fun reduce(state: MapUiState, intent: MapIntent): MapUiState {
        return when (intent) {
            is MapIntent.SelectParticipant -> state.copy(selectedParticipantIndex = intent.index)
            is MapIntent.UpdateUserName -> state.copy(userName = intent.userName)
            MapIntent.ConfirmUserName -> state
            MapIntent.ClearError -> state.copy(error = null)
            MapIntent.ShowInviteDialog -> state.copy(showInviteDialog = true)
            MapIntent.DismissInviteDialog -> state.copy(showInviteDialog = false)
            MapIntent.ShowRouteDialog -> state.copy(showRouteDialog = true)
            MapIntent.DismissRouteDialog -> state.copy(showRouteDialog = false)
        }
    }

    private fun confirmUserName() {
        val userName = _uiState.value.userName
        if (userName.isBlank()) {
            _uiState.update { it.copy(error = "이름을 입력해주세요") }
            return
        }

        _uiState.update { it.copy(showUserNameInput = false) }

        // 이름 입력 완료 후 방 참가 처리
        viewModelScope.launch {
            joinRoom(roomId, userId, userName)
                .catch { e ->
                    _uiState.update { it.copy(error = "방 참가 실패: ${e.message}") }
                    emit(null to emptyList())
                }
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationRepository.stopLocationTracking()
            roomRepository.clearCurrentRoom()
            userRepository.clearCurrentUser()
        }
    }
}
