package com.teammanduk.adego.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.usecase.BroadcastLocationUseCase
import com.teammanduk.adego.core.domain.usecase.JoinRoomUseCase
import com.teammanduk.adego.core.domain.usecase.LeaveRoomSessionUseCase
import com.teammanduk.adego.core.domain.usecase.LeaveRoomUseCase
import com.teammanduk.adego.core.domain.usecase.SearchRouteUseCase
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapIntent.ClearError
import com.teammanduk.adego.feature.map.model.MapIntent.ConfirmUserName
import com.teammanduk.adego.feature.map.model.MapIntent.DismissInviteDialog
import com.teammanduk.adego.feature.map.model.MapIntent.DismissRouteDialog
import com.teammanduk.adego.feature.map.model.MapIntent.DismissSearchPlace
import com.teammanduk.adego.feature.map.model.MapIntent.DismissSelectStartPlace
import com.teammanduk.adego.feature.map.model.MapIntent.GenerateRandomName
import com.teammanduk.adego.feature.map.model.MapIntent.LeaveRoom
import com.teammanduk.adego.feature.map.model.MapIntent.NavigateToHome
import com.teammanduk.adego.feature.map.model.MapIntent.SearchRoute
import com.teammanduk.adego.feature.map.model.MapIntent.SelectParticipant
import com.teammanduk.adego.feature.map.model.MapIntent.SelectRoute
import com.teammanduk.adego.feature.map.model.MapIntent.ShowInviteDialog
import com.teammanduk.adego.feature.map.model.MapIntent.ShowRouteDialog
import com.teammanduk.adego.feature.map.model.MapIntent.ShowSearchPlace
import com.teammanduk.adego.feature.map.model.MapIntent.ShowSelectStartPlace
import com.teammanduk.adego.feature.map.model.MapIntent.StartPlaceSelected
import com.teammanduk.adego.feature.map.model.MapIntent.UpdateUserName
import com.teammanduk.adego.feature.map.model.MapSideEffect
import com.teammanduk.adego.feature.map.model.MapUiState
import com.teammanduk.adego.feature.map.model.toUiModel
import com.teammanduk.adego.feature.map.model.toUiModels
import com.teammanduk.adego.feature.map.util.NicknameGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val joinRoom: JoinRoomUseCase,
    private val broadcastLocation: BroadcastLocationUseCase,
    private val leaveRoomSession: LeaveRoomSessionUseCase,
    private val leaveRoom: LeaveRoomUseCase,
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val searchRouteUseCase: SearchRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val _sideEffect = Channel<MapSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private val roomId: String = savedStateHandle.get<String>("roomId") ?: ""
    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    init {
        _uiState.update { it.copy(roomId = roomId, userId = userId) }

        // 방 존재 여부 확인
        viewModelScope.launch {
            roomRepository.getRoomInfo(roomId)
                .onSuccess { room ->
                    if (room == null) {
                        _uiState.update {
                            it.copy(
                                error = "존재하지 않는 방입니다. 초대 코드를 확인해주세요.",
                                isCheckingRoom = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isCheckingRoom = false) }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = "방 정보를 불러올 수 없습니다: ${e.message}",
                            isCheckingRoom = false
                        )
                    }
                }
        }
    }

    fun setSelectedRoute(route: com.teammanduk.adego.core.model.Route) {
        _uiState.update {
            it.copy(
                searchedRoutes = listOf(route),
                selectedRouteIndex = 0
            )
        }
    }

    private fun startLocationTracking() {
        viewModelScope.launch {
            broadcastLocation()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "위치 추적 실패",
                            isLocationTrackingActive = false
                        )
                    }
                }
                .collect { location ->
                    _uiState.update { it.copy(isLocationTrackingActive = true) }
                }
        }
    }

    fun onAction(intent: MapIntent) {
        when (intent) {
            is SelectParticipant -> {}
            ClearError -> {}
            NavigateToHome -> navigateToHome()
            LeaveRoom -> leaveRoomAndNavigateHome()
            ShowInviteDialog -> {}
            DismissInviteDialog -> {}
            ShowRouteDialog -> {}
            DismissRouteDialog -> {}
            ShowSelectStartPlace -> {}
            DismissSelectStartPlace -> {}
            is StartPlaceSelected -> {}
            ShowSearchPlace -> {}
            DismissSearchPlace -> {}
            SearchRoute -> searchRoute()
            is SelectRoute -> {}
            ConfirmUserName -> confirmUserName()
            is UpdateUserName -> {}
            GenerateRandomName -> {}
        }

        _uiState.update { reduce(it, intent) }
    }

    private fun navigateToHome() {
        viewModelScope.launch {
            _sideEffect.send(MapSideEffect.NavigateToHome)
        }
    }

    private fun leaveRoomAndNavigateHome() {
        viewModelScope.launch {
            leaveRoom()
                .onSuccess {
                    _sideEffect.send(MapSideEffect.NavigateToHome)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = "방 나가기 실패: ${e.message}")
                    }
                }
        }
    }

    private fun reduce(state: MapUiState, intent: MapIntent): MapUiState {
        return when (intent) {
            is SelectParticipant -> state.copy(selectedParticipantIndex = intent.index)
            ClearError -> state.copy(error = null)
            ShowInviteDialog -> state.copy(showInviteDialog = true)
            DismissInviteDialog -> state.copy(showInviteDialog = false)
            ShowRouteDialog -> state.copy(showRouteDialog = true)
            DismissRouteDialog -> state.copy(
                showRouteDialog = false,
                searchedRoutes = emptyList(),
                selectedRouteIndex = null
            )

            ShowSelectStartPlace -> state.copy(
                showSelectStartPlace = true,
                showRouteDialog = false
            )

            DismissSelectStartPlace -> state.copy(
                showSelectStartPlace = false,
                showRouteDialog = true
            )

            is StartPlaceSelected -> state.copy(
                startPlace = intent.place,
                showSelectStartPlace = false,
                showRouteDialog = true
            )

            ShowSearchPlace -> state.copy(showSearchPlace = true)
            DismissSearchPlace -> state.copy(showSearchPlace = false)
            SearchRoute -> state
            is SelectRoute -> state.copy(selectedRouteIndex = intent.routeIndex)
            ConfirmUserName -> state
            is UpdateUserName -> state.copy(userName = intent.userName)
            GenerateRandomName -> {
                val randomName = NicknameGenerator.generate()
                state.copy(userName = randomName)
            }

            NavigateToHome -> state
            LeaveRoom -> state
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

    private fun confirmUserName() {
        val userName = _uiState.value.userName

        if (userName.isBlank()) {
            _uiState.update { it.copy(error = "이름을 입력해주세요") }
            return
        }

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
                            participants = participants.toUiModels(),
                            showUserNameDialog = false
                        )
                    }

                    // 방 참가 성공 후 현재 방 설정 및 위치 추적 시작
                    if (room != null) {
                        userRepository.setCurrentUser(userId)
                        roomRepository.setCurrentRoom(roomId)
                        startLocationTracking()
                    }
                }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            leaveRoomSession()
        }
    }
}
