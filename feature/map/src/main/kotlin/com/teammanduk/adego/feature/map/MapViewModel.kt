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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 참가자의 거리 정보
 * @param participant 참가자 정보
 * @param distanceInMeters 약속장소까지의 거리 (미터)
 * @param normalizedPosition 트래킹바 위치 (0.0f = 약속장소, 1.0f = 가장 먼 사람)
 */
data class ParticipantDistance(
    val participant: Participant,
    val distanceInMeters: Int,
    val normalizedPosition: Float
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

    // 참가자 거리 정보 (트래킹바용)
    private val _participantDistances = MutableStateFlow<List<ParticipantDistance>>(emptyList())
    val participantDistances: StateFlow<List<ParticipantDistance>> = _participantDistances.asStateFlow()

    init {
        // 방 정보와 참가자 정보가 변경될 때마다 거리 계산
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(room, participants) { r, p ->
                Pair(r, p)
            }.collect { (currentRoom, currentParticipants) ->
                currentRoom?.destination?.let { destination ->
                    _participantDistances.value = calculateParticipantDistances(
                        participants = currentParticipants,
                        destination = destination
                    )
                }
            }
        }
    }

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

    /**
     * 참가자들의 약속장소까지 거리 계산
     * TODO: 나중에 route 정보가 있으면 경로 기반 거리로 변경
     */
    private fun calculateParticipantDistances(
        participants: List<Participant>,
        destination: com.teammanduk.adego.core.model.Place
    ): List<ParticipantDistance> {
        // 위치 정보가 있는 참가자만 필터링
        val participantsWithLocation = participants.filter { it.location != null }

        if (participantsWithLocation.isEmpty()) {
            return emptyList()
        }

        // 각 참가자의 거리 계산
        val distances = participantsWithLocation.map { participant ->
            val location = participant.location!!

            // TODO: route 정보가 있으면 route.distanceInMeters 사용
            // 현재는 직선 거리 계산 (Haversine)
            val distanceInMeters = calculateHaversineDistance(
                lat1 = location.latitude,
                lon1 = location.longitude,
                lat2 = destination.latitude,
                lon2 = destination.longitude
            )

            ParticipantDistance(
                participant = participant,
                distanceInMeters = distanceInMeters,
                normalizedPosition = 0f // 아래에서 계산
            )
        }

        // 가장 먼 거리 찾기
        val maxDistance = distances.maxOfOrNull { it.distanceInMeters } ?: 1

        // normalized position 계산 (0.0f ~ 1.0f)
        return distances.map { distance ->
            distance.copy(
                normalizedPosition = if (maxDistance > 0) {
                    distance.distanceInMeters.toFloat() / maxDistance.toFloat()
                } else {
                    0f
                }
            )
        }
    }

    /**
     * Haversine 공식을 사용한 두 지점 간 직선 거리 계산 (미터)
     */
    private fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val earthRadiusMeters = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadiusMeters * c).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            locationRepository.stopLocationTracking()
        }
    }
}
