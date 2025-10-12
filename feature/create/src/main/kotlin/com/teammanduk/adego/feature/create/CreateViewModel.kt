package com.teammanduk.adego.feature.create

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.usecase.GetSelectedPlaceUseCase
import com.teammanduk.adego.core.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CreatedRoomInfo(
    val roomId: String,
    val userId: String,
    val hour: Int,
    val minute: Int
)

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val getSelectedPlaceUseCase: GetSelectedPlaceUseCase,
    private val roomRepository: RoomRepository
) : ViewModel() {

    val selectedPlace: StateFlow<Place?> = getSelectedPlaceUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createdRoomInfo = MutableStateFlow<CreatedRoomInfo?>(null)
    val createdRoomInfo: StateFlow<CreatedRoomInfo?> = _createdRoomInfo.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 방 생성
     */
    fun createRoom(
        roomName: String,
        dateMillis: Long,
        hour: Int,
        minute: Int,
        userId: String,  // TODO: 실제 사용자 ID로 교체
        userName: String // TODO: 실제 사용자 이름으로 교체
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "[ViewModel] 방 생성 시작: roomName=$roomName, userId=$userId")
                _isCreating.value = true
                _error.value = null

                val place = selectedPlace.value
                    ?: throw IllegalStateException("장소가 선택되지 않았습니다")

                Log.d(TAG, "[ViewModel] 선택된 장소: ${place.name}")

                // 날짜/시간 포맷팅
                val dateTime = formatDateTime(dateMillis, hour, minute)
                Log.d(TAG, "[ViewModel] 포맷팅된 날짜/시간: $dateTime")

                // 방 생성
                Log.d(TAG, "[ViewModel] RoomRepository.createRoom() 호출 시작")
                val result = roomRepository.createRoom(
                    roomName = roomName,
                    destination = place,
                    dateTime = dateTime,
                    userId = userId,
                    userName = userName
                )

                result.onSuccess { roomId ->
                    Log.d(TAG, "[ViewModel] 방 생성 성공! roomId=$roomId")
                    _createdRoomInfo.value = CreatedRoomInfo(
                        roomId = roomId,
                        userId = userId,
                        hour = hour,
                        minute = minute
                    )
                }.onFailure { exception ->
                    Log.e(TAG, "[ViewModel] 방 생성 실패", exception)
                    _error.value = exception.message ?: "방 생성에 실패했습니다"
                }
            } catch (e: Exception) {
                Log.e(TAG, "[ViewModel] 방 생성 중 예외 발생", e)
                _error.value = e.message ?: "알 수 없는 오류가 발생했습니다"
            } finally {
                _isCreating.value = false
                Log.d(TAG, "[ViewModel] 방 생성 프로세스 종료")
            }
        }
    }

    companion object {
        private const val TAG = "AdegoRoom"
    }

    private fun formatDateTime(dateMillis: Long, hour: Int, minute: Int): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val date = dateFormat.format(Date(dateMillis))
        val time = String.format("%02d:%02d:00", hour, minute)
        return "${date}T$time"
    }

    fun clearCreatedRoomInfo() {
        _createdRoomInfo.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
