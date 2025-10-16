# MapScreen 진입 흐름

## 개요
사용자가 방에 참가하여 지도 화면에 진입하는 전체 흐름을 설명합니다.

## 진입 조건 체크 순서

MapRoute에서 다음 조건들을 순차적으로 체크하여 적절한 화면을 표시합니다:

```kotlin
when {
    uiState.isCheckingRoom -> "방 정보를 확인하는 중..." 로딩 화면
    uiState.error != null -> 에러 화면 표시 (버튼으로 홈 이동)
    uiState.showUserNameDialog -> UserNameInputDialog 표시
    uiState.currentUser?.location == null -> "현재 위치를 가져오는 중..." 로딩 화면
    else -> MapScreen 표시
}
```

## 0단계: 방 존재 여부 확인 (자동)

### 조건
- ViewModel init 시점에 자동 실행
- Deep link 또는 방 생성 후 진입

### 동작
1. `roomRepository.getRoomInfo(roomId)` 호출하여 방 정보 조회
2. 방이 존재하지 않으면 (`room == null`):
   - `error = "존재하지 않는 방입니다. 초대 코드를 확인해주세요."`
   - `isCheckingRoom = false`
   - ErrorScreen 표시
3. 방 정보 조회 실패 시:
   - `error = "방 정보를 불러올 수 없습니다: ${e.message}"`
   - `isCheckingRoom = false`
   - ErrorScreen 표시

### ViewModel 처리
```kotlin
init {
    _uiState.update { it.copy(roomId = roomId, userId = userId) }

    // 방 존재 여부 확인 (setCurrentUser/setCurrentRoom은 방 참가 후에만)
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
```

### 에러 처리 (ErrorScreen)
- 에러 메시지와 "홈으로 돌아가기" 버튼 표시
- 버튼 클릭 시:
  1. `MapIntent.NavigateToHome` 발생
  2. `MapViewModel.navigateToHome()` 호출
  3. `MapSideEffect.NavigateToHome` 발생
  4. `onNavigateToHome()` 콜백 실행 (백스택 클리어하며 홈으로 이동)

```kotlin
ErrorScreen(
    message = uiState.error!!,
    onAction = { viewModel.onAction(MapIntent.NavigateToHome) }
)
```

### 다음 단계로 이동
- 방이 존재하면 (`isCheckingRoom = false`) 1단계(사용자 이름 입력)로 진행
- 방이 존재하지 않으면 ErrorScreen 표시 후 사용자가 버튼 클릭 시 홈으로 이동

## 1단계: 사용자 이름 입력

### 조건
- `uiState.isCheckingRoom == false`
- `uiState.error == null`
- `uiState.showUserNameDialog == true` (기본값)

### 동작
1. `UserNameInputDialog` 표시
2. LaunchedEffect로 자동으로 랜덤 닉네임 생성 (`NicknameGenerator.generate()`)
3. 사용자가 이름 수정 가능 (Refresh 버튼으로 재생성 가능)
4. "확인" 버튼 클릭 시 `MapIntent.ConfirmUserName` 발생

### ViewModel 처리
```kotlin
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
                        showUserNameDialog = false  // 다이얼로그 닫기
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
```

### 핵심 변경사항
- **방 참가 성공 후에만** `setCurrentUser()`, `setCurrentRoom()`, `startLocationTracking()` 호출
- 이전에는 init에서 호출되어 방이 없어도 설정되는 문제가 있었음
- 이제 제대로 방에 참가한 후에만 위치 추적 시작

### 다음 단계로 이동
- `showUserNameDialog = false`로 설정되어 다이얼로그 닫힘
- `startLocationTracking()` 호출되어 2단계로 이동

## 2단계: 현재 위치 로딩

### 조건
- `uiState.showUserNameDialog == false`
- `uiState.currentUser?.location == null`

### 동작
1. "현재 위치를 가져오는 중..." 로딩 화면 표시
2. `PermissionRequester`가 위치 권한 확인 (자동)
3. `confirmUserName()`에서 이미 `startLocationTracking()` 호출됨

### ViewModel 처리
```kotlin
fun startLocationTracking() {
    viewModelScope.launch {
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
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "위치 추적 시작 실패: ${e.message}") }
        }
    }
}
```

### 위치 데이터 흐름
1. `getCurrentLocation()` 또는 `trackAndUpdateLocation()`으로 위치 획득
2. `roomRepository.updateMyLocation(userId, location)` 호출
3. Firebase에 위치 업데이트
4. `JoinRoomUseCase`의 실시간 구독을 통해 `participants` 리스트 업데이트
5. `currentUser.location` computed property가 자동으로 업데이트

```kotlin
// MapUiState.kt
val currentUser: ParticipantUiModel?
    get() = participants.find { it.userId == userId }
```

### 다음 단계로 이동
- `currentUser?.location != null`이 되면 3단계로 이동

## 3단계: MapScreen 표시

### 조건
- `uiState.showUserNameDialog == false`
- `uiState.currentUser?.location != null`

### 동작
1. 실제 지도 화면(`MapScreen`) 표시
2. 초기 카메라 위치를 `currentUser.location`으로 설정

```kotlin
val cameraPositionState = rememberCameraPositionState {
    // currentUser 위치를 우선적으로 사용하고, 없으면 선택된 참가자 위치 사용
    val initialLocation = uiState.currentUser?.location
        ?: uiState.participants.getOrNull(uiState.selectedParticipantIndex)?.location
    if (initialLocation != null) {
        position = CameraPosition.fromLatLngZoom(
            LatLng(initialLocation.latitude, initialLocation.longitude), 15f
        )
    }
}
```

3. 참가자 마커, 목적지 마커, 경로 등 표시
4. 실시간 위치 업데이트 구독 계속 유지

## 데이터 구조

### MapUiState
```kotlin
data class MapUiState(
    val roomId: String = "",
    val userId: String = "",
    val room: RoomUiModel? = null,
    val participants: List<ParticipantUiModel> = emptyList(),
    val selectedParticipantIndex: Int = 0,
    val isLocationTrackingActive: Boolean = false,
    val isCheckingRoom: Boolean = true,
    val error: String? = null,
    val showInviteDialog: Boolean = false,
    val showRouteDialog: Boolean = false,
    val showSelectStartPlace: Boolean = false,
    val showSearchPlace: Boolean = false,
    val startPlace: com.teammanduk.adego.core.model.Place? = null,
    val searchedRoutes: List<com.teammanduk.adego.core.model.Route> = emptyList(),
    val selectedRouteIndex: Int? = null,
    val isSearchingRoute: Boolean = false,
    val userName: String = "",
    val showUserNameDialog: Boolean = true,
) {
    val currentUser: ParticipantUiModel?
        get() = participants.find { it.userId == userId }
}
```

### 주요 Computed Property
- `currentUser`: 현재 사용자의 참가자 정보를 participants에서 찾아 반환
- `currentUser.location`: 현재 사용자의 위치 정보 (별도 필드 없이 participants에서 자동 추출)

## Navigation 처리

### NavigateToHome SideEffect
```kotlin
sealed interface MapSideEffect {
    data object NavigateToHome : MapSideEffect
}
```

### 흐름
1. ErrorScreen 버튼 클릭 또는 사용자 Intent
2. `MapIntent.NavigateToHome` 발생
3. `MapViewModel.navigateToHome()` 실행:
   ```kotlin
   private fun navigateToHome() {
       viewModelScope.launch {
           _sideEffect.send(MapSideEffect.NavigateToHome)
       }
   }
   ```
4. MapRoute에서 SideEffect 수신:
   ```kotlin
   LaunchedEffect(Unit) {
       viewModel.sideEffect.collect { sideEffect ->
           when (sideEffect) {
               is MapSideEffect.NavigateToHome -> {
                   onNavigateToHome()  // 백스택 클리어하며 홈으로 이동
               }
           }
       }
   }
   ```

## 에러 처리

### 방 존재 확인 실패
- "존재하지 않는 방입니다. 초대 코드를 확인해주세요." 또는 "방 정보를 불러올 수 없습니다: ${e.message}"
- ErrorScreen 표시 → 사용자 버튼 클릭 → 홈으로 이동

### 위치 로딩 실패
- 10회 재시도 후 실패 시 에러 메시지 표시
- "위치를 가져올 수 없습니다. 위치 서비스를 확인해주세요."

### 방 참가 실패
- JoinRoomUseCase에서 에러 발생 시 에러 메시지 표시
- "방 참가 실패: ${e.message}"

### 위치 추적 실패
- 실시간 위치 업데이트 중 에러 발생 시 에러 메시지 표시
- "위치 추적 실패: ${e.message}"
- `isLocationTrackingActive = false`로 설정

## 주요 설계 원칙

1. **단일 데이터 소스**: `myLocation`을 별도로 관리하지 않고 `participants`에서 찾아 사용
2. **UI 계산 최소화**: computed property를 통해 UI에서 직접 계산하지 않음
3. **명확한 상태 관리**: `showUserNameDialog`, `isCheckingRoom` 플래그로 화면 제어
4. **자동 재시도**: 위치 로딩 실패 시 자동으로 재시도하여 사용자 경험 개선
5. **실시간 동기화**: Firebase 실시간 구독을 통해 모든 참가자 정보 자동 동기화
6. **적절한 타이밍**: Repository 설정과 위치 추적은 방 참가 성공 후에만 실행
7. **사용자 중심 Navigation**: 에러 발생 시 자동 이동이 아닌 사용자 버튼 클릭 후 이동

## 전체 흐름 요약

```
[ViewModel init]
    ↓
[방 존재 여부 확인]
    ↓
[방 없음] → ErrorScreen → 버튼 클릭 → 홈으로 이동 (백스택 클리어)
    ↓
[방 존재]
    ↓
[이름 입력 다이얼로그]
    ↓
[방 참가 (joinRoom)]
    ↓
[setCurrentUser + setCurrentRoom + startLocationTracking]
    ↓
[위치 로딩 중...]
    ↓
[위치 획득]
    ↓
[MapScreen 표시]
```
