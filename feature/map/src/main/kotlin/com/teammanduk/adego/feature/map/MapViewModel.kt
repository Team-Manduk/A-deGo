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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    // 방 정보
    val room: StateFlow<Room?> = roomRepository.observeRoom(roomId)
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 참가자 목록
    val participants: StateFlow<List<Participant>> = roomRepository.observeParticipants(roomId)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 현재 선택된 참가자 인덱스
    private val _selectedParticipantIndex = MutableStateFlow(0)
    val selectedParticipantIndex: StateFlow<Int> = _selectedParticipantIndex.asStateFlow()

    // 에러 상태
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 위치 추적 활성화 여부
    private val _isLocationTrackingActive = MutableStateFlow(false)
    val isLocationTrackingActive: StateFlow<Boolean> = _isLocationTrackingActive.asStateFlow()

    // 초기 위치 로딩 상태
    private val _isInitialLocationLoaded = MutableStateFlow(false)
    val isInitialLocationLoaded: StateFlow<Boolean> = _isInitialLocationLoaded.asStateFlow()

    // 위치 추적 시작 (외부에서 호출 가능하도록 public으로 변경)
    fun startLocationTracking() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "[MapViewModel] 위치 추적 시작 - roomId: $roomId, userId: $userId")
                locationRepository.observeLocationUpdates()
                    .catch { e ->
                        Log.e(TAG, "[MapViewModel] 위치 추적 실패", e)
                        _error.value = "위치 추적 실패: ${e.message}"
                        _isLocationTrackingActive.value = false
                    }
                    .collect { location ->
                        Log.d(TAG, "[MapViewModel] 위치 업데이트 수신: lat=${location.latitude}, lng=${location.longitude}")
                        _isLocationTrackingActive.value = true
                        if (!_isInitialLocationLoaded.value) {
                            Log.d(TAG, "[MapViewModel] 초기 위치 로드 완료")
                            _isInitialLocationLoaded.value = true
                        }
                        updateMyLocation(location)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "[MapViewModel] 위치 추적 시작 실패", e)
                _error.value = "위치 추적 시작 실패: ${e.message}"
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
            _error.value = "위치 업데이트 실패: ${e.message}"
        }
    }

    // 선택된 참가자 인덱스 변경
    fun onParticipantSelected(index: Int) {
        _selectedParticipantIndex.value = index
    }

    // 에러 메시지 초기화
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationRepository.stopLocationTracking()
        }
    }
}
