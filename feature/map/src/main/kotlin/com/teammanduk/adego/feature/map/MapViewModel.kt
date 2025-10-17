package com.teammanduk.adego.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.usecase.DebugSetLocationUseCase
import com.teammanduk.adego.core.domain.usecase.JoinRoomUseCase
import com.teammanduk.adego.core.domain.usecase.SearchRouteUseCase
import com.teammanduk.adego.core.domain.usecase.TrackAndUpdateLocationUseCase
import com.teammanduk.adego.core.domain.usecase.UpdateLocationAndEtaUseCase
import com.teammanduk.adego.feature.map.model.MapIntent
import com.teammanduk.adego.feature.map.model.MapIntent.ClearError
import com.teammanduk.adego.feature.map.model.MapIntent.ConfirmUserName
import com.teammanduk.adego.feature.map.model.MapIntent.DismissInviteDialog
import com.teammanduk.adego.feature.map.model.MapIntent.DismissRouteDialog
import com.teammanduk.adego.feature.map.model.MapIntent.DismissSearchPlace
import com.teammanduk.adego.feature.map.model.MapIntent.DismissSelectStartPlace
import com.teammanduk.adego.feature.map.model.MapIntent.GenerateRandomName
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
import com.teammanduk.adego.feature.map.model.MapIntent.DebugSetLocation
import com.teammanduk.adego.feature.map.model.MapSideEffect
import com.teammanduk.adego.feature.map.model.MapUiState
import com.teammanduk.adego.feature.map.model.toUiModel
import com.teammanduk.adego.feature.map.model.toUiModels
import com.teammanduk.adego.feature.map.util.NicknameGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val trackAndUpdateLocation: TrackAndUpdateLocationUseCase,
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val locationRepository: LocationRepository,
    private val searchRouteUseCase: SearchRouteUseCase,
    private val updateLocationAndEtaUseCase: UpdateLocationAndEtaUseCase,
    private val debugSetLocationUseCase: DebugSetLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val _sideEffect = Channel<MapSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private val roomId: String = savedStateHandle.get<String>("roomId") ?: ""
    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    // ETA 계산 관련 상태
    private var etaCalculationJob: Job? = null
    private var lastEtaCalculationTime = 0L
    private val ETA_CALCULATION_INTERVAL_MS = 30000L // 30초마다만 계산 (Firebase 부하 감소)

    // 위치 추적 Job (중복 구독 방지)
    private var locationTrackingJob: Job? = null

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

    fun startLocationTracking() {
        // 이미 위치 추적 중이면 중복 실행 방지
        if (locationTrackingJob?.isActive == true) {
            android.util.Log.d("MapViewModel", "위치 추적이 이미 실행 중입니다. 중복 호출 무시.")
            return
        }

        locationTrackingJob = viewModelScope.launch {
            try {
                // 1. 현재 위치를 가져올 때까지 재시도 (최대 10회, 2초 간격)
                var retryCount = 0
                var locationObtained = false

                while (!locationObtained && retryCount < 10) {
                    locationRepository.getCurrentLocation()
                        .onSuccess { location ->
                            // 초기 위치를 Firebase에 즉시 업데이트 (participants에 자동 반영됨)
                            roomRepository.updateMyLocation(userId, location)
                            locationObtained = true
                        }
                        .onFailure { e ->
                            retryCount++
                            if (retryCount < 10) {
                                kotlinx.coroutines.delay(2000) // 2초 대기 후 재시도
                            }
                        }
                }

                // 위치를 가져오지 못한 경우 에러 표시
                if (!locationObtained) {
                    _uiState.update {
                        it.copy(error = "위치를 가져올 수 없습니다. 위치 서비스를 확인해주세요.")
                    }
                    return@launch
                }

                android.util.Log.d("MapViewModel", "위치 추적 시작 - 실시간 업데이트 구독")

                // 2. 위치를 성공적으로 가져온 후 실시간 위치 업데이트 구독
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
                            currentState.copy(isLocationTrackingActive = true)
                        }

                        // 3. 위치 업데이트 시 ETA 계산 및 업데이트
                        calculateAndUpdateEta(location)
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "위치 추적 시작 실패: ${e.message}") }
            }
        }
    }

    fun onAction(intent: MapIntent) {
        when (intent) {
            is SelectParticipant -> {}
            ClearError -> {}
            NavigateToHome -> navigateToHome()
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
            is DebugSetLocation -> handleDebugSetLocation(intent.latitude, intent.longitude)
        }

        _uiState.update { reduce(it, intent) }
    }

    private fun navigateToHome() {
        viewModelScope.launch {
            _sideEffect.send(MapSideEffect.NavigateToHome)
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
            is DebugSetLocation -> state // 상태 변경 없음
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


    /**
     * 위치 업데이트 시 ETA 계산 및 Firebase 업데이트
     * - 디바운싱: 30초마다만 계산
     * - 중복 계산 방지
     * - 비즈니스 로직은 UpdateLocationAndEtaUseCase에 위임
     */
    private fun calculateAndUpdateEta(currentLocation: com.teammanduk.adego.core.model.ParticipantLocation) {
        val currentTime = System.currentTimeMillis()

        // 마지막 계산으로부터 30초가 지나지 않았으면 스킵 (디바운싱)
        if (currentTime - lastEtaCalculationTime < ETA_CALCULATION_INTERVAL_MS) {
            return
        }

        // 이미 계산 중이면 스킵
        if (etaCalculationJob?.isActive == true) {
            return
        }

        etaCalculationJob = viewModelScope.launch {
            val selectedRoute = _uiState.value.searchedRoutes.getOrNull(
                _uiState.value.selectedRouteIndex ?: -1
            )

            // UseCase에 위임
            updateLocationAndEtaUseCase(userId, currentLocation, selectedRoute)
                .onSuccess {
                    lastEtaCalculationTime = currentTime
                }
                .onFailure { e ->
                    android.util.Log.e("MapViewModel", "위치/ETA 업데이트 실패: ${e.message}", e)
                }
        }
    }

    /**
     * 디버그: 지도 클릭으로 위치 설정
     * - 비즈니스 로직은 DebugSetLocationUseCase에 위임
     * - 디바운싱 없이 즉시 실행
     */
    private fun handleDebugSetLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val selectedRoute = _uiState.value.searchedRoutes.getOrNull(
                _uiState.value.selectedRouteIndex ?: -1
            )

            // UseCase에 위임
            debugSetLocationUseCase(userId, latitude, longitude, selectedRoute)
                .onSuccess {
                    android.util.Log.d("MapViewModel", "🔧 DEBUG: 위치 설정 완료 - lat: $latitude, lng: $longitude")
                }
                .onFailure { e ->
                    android.util.Log.e("MapViewModel", "🔧 DEBUG: 위치 설정 실패: ${e.message}", e)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // 위치 추적 Job 취소
        locationTrackingJob?.cancel()
        etaCalculationJob?.cancel()

        viewModelScope.launch {
            locationRepository.stopLocationTracking()
            roomRepository.clearCurrentRoom()
            userRepository.clearCurrentUser()
        }
    }
}
