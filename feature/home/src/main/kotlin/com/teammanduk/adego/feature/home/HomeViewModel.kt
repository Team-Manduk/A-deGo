package com.teammanduk.adego.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "A-degoLogTag"

data class RoomJoinInfo(
    val roomId: String,
    val userId: String,
    val hour: Int,
    val minute: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _joinedRoomInfo = MutableStateFlow<RoomJoinInfo?>(null)
    val joinedRoomInfo: StateFlow<RoomJoinInfo?> = _joinedRoomInfo.asStateFlow()

    fun joinRoomWithCode(inviteCode: String) {
        Log.d(TAG, "[HomeViewModel] 초대코드로 방 참여 시도: $inviteCode")

        viewModelScope.launch {
            _isJoining.value = true

            try {
                // 1. 방 존재 여부 확인
                val roomResult = roomRepository.getRoomInfo(inviteCode)

                roomResult.fold(
                    onSuccess = { room ->
                        if (room == null) {
                            Log.w(TAG, "[HomeViewModel] 방을 찾을 수 없음: $inviteCode")
                            _error.value = "존재하지 않는 초대 코드입니다."
                            _isJoining.value = false
                            return@launch
                        }

                        Log.d(TAG, "[HomeViewModel] 방 정보 조회 성공: ${room.roomName}")

                        // 2. 임시 userId 생성
                        val tempUserId = "user_${System.currentTimeMillis()}"
                        val tempUserName = "사용자"

                        // 3. 방 참여
                        Log.d(TAG, "[HomeViewModel] 방 참여 시도 - userId: $tempUserId, userName: $tempUserName")
                        val joinResult = roomRepository.joinRoom(
                            roomId = inviteCode,
                            userId = tempUserId,
                            userName = tempUserName
                        )

                        joinResult.fold(
                            onSuccess = {
                                Log.d(TAG, "[HomeViewModel] 방 참여 성공")

                                // 시간 정보 추출 (예: "2025-01-13 14:30" -> hour=14, minute=30)
                                val timeParts = room.dateTime.split(" ")
                                val hourMinute = if (timeParts.size >= 2) {
                                    val time = timeParts[1].split(":")
                                    if (time.size >= 2) {
                                        Pair(time[0].toIntOrNull() ?: 0, time[1].toIntOrNull() ?: 0)
                                    } else {
                                        Pair(0, 0)
                                    }
                                } else {
                                    Pair(0, 0)
                                }

                                _joinedRoomInfo.value = RoomJoinInfo(
                                    roomId = inviteCode,
                                    userId = tempUserId,
                                    hour = hourMinute.first,
                                    minute = hourMinute.second
                                )
                            },
                            onFailure = { exception ->
                                Log.e(TAG, "[HomeViewModel] 방 참여 실패", exception)
                                _error.value = "방 참여에 실패했습니다: ${exception.message}"
                            }
                        )
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "[HomeViewModel] 방 정보 조회 실패", exception)
                        _error.value = "방 정보를 가져올 수 없습니다: ${exception.message}"
                    }
                )
            } finally {
                _isJoining.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearJoinedRoomInfo() {
        _joinedRoomInfo.value = null
    }
}
