package com.teammanduk.adego.feature.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.Room
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val showInviteDialog: Boolean = false
)

@HiltViewModel(assistedFactory = MapViewModel.Factory::class)
class MapViewModel @AssistedInject constructor(
    @Assisted("roomId") private val roomId: String,
    @Assisted("userId") val userId: String,  // public으로 변경
    private val roomRepository: RoomRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("roomId") roomId: String,
            @Assisted("userId") userId: String
        ): MapViewModel
    }

    // UI 상태 통합
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = combine(
        roomRepository.observeRoom(roomId).catch { emit(null) },
        roomRepository.observeParticipants(roomId).catch { emit(emptyList()) },
        _uiState
    ) { room, participants, currentState ->
        currentState.copy(
            room = room,
            participants = participants
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    // 위치 추적 시작
    fun startLocationTracking() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "[MapViewModel] 위치 추적 시작 - roomId: $roomId, userId: $userId")
                locationRepository.observeLocationUpdates()
                    .catch { e ->
                        Log.e(TAG, "[MapViewModel] 위치 추적 실패", e)
                        _uiState.update { it.copy(
                            error = "위치 추적 실패: ${e.message}",
                            isLocationTrackingActive = false
                        ) }
                    }
                    .collect { location ->
                        Log.d(TAG, "[MapViewModel] 위치 업데이트 수신: lat=${location.latitude}, lng=${location.longitude}")

                        _uiState.update { currentState ->
                            currentState.copy(
                                isLocationTrackingActive = true,
                                isInitialLocationLoaded = if (!currentState.isInitialLocationLoaded) {
                                    Log.d(TAG, "[MapViewModel] 초기 위치 로드 완료")
                                    true
                                } else {
                                    currentState.isInitialLocationLoaded
                                }
                            )
                        }

                        updateMyLocation(location)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "[MapViewModel] 위치 추적 시작 실패", e)
                _uiState.update { it.copy(error = "위치 추적 시작 실패: ${e.message}") }
            }
        }
    }

    companion object {
        private const val TAG = "A-degoLogTag"
    }

    // 내 위치 Firebase에 업데이트
    private suspend fun updateMyLocation(location: ParticipantLocation) {
        try {
            Log.d(TAG, "[MapViewModel] Firebase에 위치 업데이트 중...")
            roomRepository.updateMyLocation(
                roomId = roomId,
                userId = userId,
                location = location
            )
            Log.d(TAG, "[MapViewModel] Firebase 위치 업데이트 완료")
        } catch (e: Exception) {
            Log.e(TAG, "[MapViewModel] 위치 업데이트 실패", e)
            _uiState.update { it.copy(error = "위치 업데이트 실패: ${e.message}") }
        }
    }

    // MVI 패턴: Intent 처리를 위한 단일 진입점
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
        }
    }

    // MVI 패턴: 순수 함수로 상태 변환 처리
    private fun reduce(state: MapUiState, intent: MapIntent): MapUiState {
        return when (intent) {
            is MapIntent.SelectParticipant -> state.copy(selectedParticipantIndex = intent.index)
            MapIntent.ClearError -> state.copy(error = null)
            MapIntent.ShowInviteDialog -> state.copy(showInviteDialog = true)
            MapIntent.DismissInviteDialog -> state.copy(showInviteDialog = false)
        }
    }

    // 하위 호환성을 위한 래퍼 함수들 (추후 제거 예정)
    @Deprecated("Use onAction(MapIntent.SelectParticipant) instead")
    fun onParticipantSelected(index: Int) {
        onAction(MapIntent.SelectParticipant(index))
    }

    @Deprecated("Use onAction(MapIntent.ClearError) instead")
    fun clearError() {
        onAction(MapIntent.ClearError)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationRepository.stopLocationTracking()
        }
    }
}
