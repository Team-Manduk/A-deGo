# Feature: Map

실시간 위치 추적 및 참가자 시각화를 담당하는 핵심 Feature 모듈입니다.

## 개요

Map 모듈은 A-deGo의 메인 화면으로, Google Maps 위에서 모든 참가자의 실시간 위치를 시각화하고, 경로 정보를 표시하며, 사용자 간 상호작용을 제공합니다.

## 주요 기능

### 1. 실시간 위치 추적

- **Dual-Path 위치 업데이트**: 자세한 내용은 [LOCATION_FLOW.md](LOCATION_FLOW.md) 참조
  - 빠른 경로: GPS → UI 직접 업데이트 (~32ms)
  - 느린 경로: GPS → Firebase 업데이트 (~600ms, 필터링 적용)

- **백그라운드 추적**: Foreground Service를 통한 지속적인 위치 업데이트

- **배터리 최적화**:
  - 25m 이동 시에만 업데이트
  - 또는 30초 경과 시 업데이트
  - 첫 위치는 항상 업데이트

### 2. 참가자 시각화

- **지도 마커**: 각 참가자의 현재 위치를 색상별 마커로 표시
- **참가자 카드**: 하단 수평 스크롤 카드로 참가자 정보 표시
  - 이름, 프로필 색상
  - 목적지까지 남은 거리
  - 이동 상태 (미출발, 준비, 이동중, 도착임박, 도착)
- **카메라 추적**: 선택된 참가자 또는 내 위치 중심으로 카메라 이동

### 3. 경로 표시

- **Polyline**: 선택된 대중교통 경로를 지도에 표시
- **경로 정보**: 소요 시간, 거리, 요금
- **경로 재선택**: "경로 다시 선택" 버튼

### 4. 이동 상태 자동 감지

**MovementStatus** (거리 기반 자동 계산):
- `ARRIVED` (≤100m) - 목적지 도착
- `ARRIVING_SOON` (≤500m) - 도착 임박
- `IN_PROGRESS` - 이동 중
- `READY` (≤200m from start) - 출발 준비
- `NOT_STARTED` - 경로 미선택

### 5. 방 관리

- **방 정보 표시**: 상단에 방 이름, 목적지, 시간 표시
- **초대 코드 공유**: 다이얼로그를 통한 코드 공유
- **방 나가기**: 확인 다이얼로그 후 Firebase에서 참가자 제거

## 구조

### Components

#### MapRoute (Composable)
- ViewModel 상태 관찰
- 위치 권한 요청
- 위치 추적 시작/중지
- SideEffect 처리 (네비게이션)

#### MapScreen (Composable)
- Google Maps 렌더링
- 참가자 마커 표시
- Polyline 경로 표시
- UI 오버레이 (상단바, 참가자 카드, 버튼)

#### MapViewModel
- MVI 패턴 구현
- 실시간 위치 업데이트 처리
- 방 및 참가자 정보 동기화
- 이동 상태 계산

## 데이터 플로우

### 방 참여 플로우

```
MapScreen 진입 (roomId, userId, userName?)
    ↓
MapViewModel.init
    ↓
userName == null?
├─ YES → showUserNameDialog = true
│        사용자가 이름 입력
│        ↓
│        MapViewModel.confirmUserName()
│        ↓
└─ NO  → 바로 참여

JoinRoomUseCase.invoke(roomId, userId, userName)
    ↓
roomRepository.joinRoom()
    ↓
Firebase: participants/{roomId}/{userId} 생성
    ↓
observeCurrentRoom() + observeCurrentParticipants()
    ↓
combine(room, participants) → MapUiState
```

### 실시간 위치 업데이트 플로우

```
[UI Path - 빠름]
FusedLocationProvider
    ↓
locationRepository.observeLocationUpdates()
    ↓
MapViewModel collects
    ↓
_uiState.update { myCurrentLocation = location }
    ↓
Map camera moves to my location (~32ms)

[Firebase Path - 느림]
LocationTrackingService (Foreground)
    ↓
BroadcastLocationUseCase
    ↓
UpdateLocationUseCase (필터링: 25m or 30s)
    ↓
roomRepository.updateMyLocation()
    ↓
Firebase: participants/{roomId}/{userId}/location
    ↓
FirebaseRoomDataSource.observeParticipants()
    ↓
ValueEventListener triggers
    ↓
MapViewModel.participants updates (~600ms)
```

## UI State

### MapUiState

```kotlin
data class MapUiState(
    val room: RoomUiModel? = null,                      // 방 정보
    val participants: List<ParticipantUiModel> = emptyList(),  // 참가자 목록
    val myCurrentLocation: ParticipantLocation? = null, // 내 현재 위치 (빠른 경로)
    val selectedParticipantIndex: Int = -1,             // 선택된 참가자 인덱스
    val showUserNameDialog: Boolean = false,            // 이름 입력 다이얼로그
    val showInviteDialog: Boolean = false,              // 초대 코드 다이얼로그
    val showLeaveDialog: Boolean = false,               // 방 나가기 다이얼로그
    val isLoading: Boolean = false,                     // 로딩 상태
    val error: String? = null                           // 에러 메시지
)
```

### Computed Properties

```kotlin
// 내 정보 (participants 리스트에서 추출)
val currentUser: ParticipantUiModel?
    get() = participants.find { it.userId == userId }

// 선택된 참가자
val selectedParticipant: ParticipantUiModel?
    get() = participants.getOrNull(selectedParticipantIndex)
```

## Dependencies

### Domain Layer
```kotlin
- JoinRoomUseCase                      // 방 참여
- LeaveRoomUseCase                     // 방 나가기
- CalculateMovementStatusUseCase       // 이동 상태 계산
```

### Data Layer
```kotlin
- RoomRepository                       // 방 정보 및 참가자 관리
- LocationRepository                   // 위치 정보
- SelectedRouteRepository              // 선택된 경로
```

### Sync Layer
```kotlin
- LocationTrackingManager              // 위치 추적 서비스 제어
```

## 주요 클래스

### MapViewModel

**파일 위치**: `feature/map/src/main/kotlin/com/teammanduk/adego/feature/map/MapViewModel.kt`

**주요 메서드**:
- `onAction(intent: MapIntent)`: Intent 처리
- `checkRoomAndShowDialog()`: 방 존재 여부 확인
- `confirmUserName(name: String)`: 사용자 이름 확인 후 방 참여
- `selectRoute()`: 경로 선택 화면으로 이동
- `leaveRoom()`: 방 나가기

### ParticipantUiModel

```kotlin
data class ParticipantUiModel(
    val userId: String,
    val name: String,
    val profileColor: String,                   // 마커 색상
    val location: ParticipantLocation?,
    val route: ParticipantRoute?,
    val distanceToDestination: Int?,            // 목적지까지 거리 (m)
    val movementStatus: MovementStatus          // 이동 상태
)
```

### RouteUiModel

```kotlin
data class RouteUiModel(
    val totalTime: Int,                         // 총 소요 시간 (분)
    val totalDistance: Int,                     // 총 거리 (m)
    val fare: Int,                              // 요금 (원)
    val polylineCoordinates: List<LatLng>       // Polyline 좌표
)
```

## MVI Pattern

### Intent

```kotlin
sealed interface MapIntent {
    data object ShowInviteDialog : MapIntent
    data object DismissInviteDialog : MapIntent
    data object ShowLeaveDialog : MapIntent
    data object DismissLeaveDialog : MapIntent
    data object LeaveRoom : MapIntent
    data class ConfirmUserName(val name: String) : MapIntent
    data class SelectParticipant(val index: Int) : MapIntent
    data object SelectRoute : MapIntent
}
```

### SideEffect

```kotlin
sealed interface MapSideEffect {
    data object NavigateToHome : MapSideEffect
    data class NavigateToSelectStartPlace(
        val roomId: String,
        val userId: String,
        val destLat: Double,
        val destLng: Double
    ) : MapSideEffect
}
```

## 사용 예시

### 방 참여 시나리오

```kotlin
// Deep link로 들어온 경우 (userName = null)
MapScreen(roomId = "ABC123", userId = "user_123", userName = null)

// MapViewModel:
init {
    if (userName.isNullOrEmpty()) {
        _uiState.update { it.copy(showUserNameDialog = true) }
    } else {
        joinRoom(userName)
    }
}

// 사용자가 "홍길동" 입력 후 확인
onAction(MapIntent.ConfirmUserName("홍길동"))

// joinRoom() 실행:
JoinRoomUseCase(roomId, userId, "홍길동")
→ Firebase participants/{ABC123}/user_123 생성
→ 실시간 동기화 시작
```

### 경로 선택 시나리오

```kotlin
// "경로 선택" 버튼 클릭
onAction(MapIntent.SelectRoute)

// MapViewModel:
val destPlace = _uiState.value.room?.destination
_sideEffect.send(NavigateToSelectStartPlace(
    roomId = roomId,
    userId = userId,
    destLat = destPlace.latitude,
    destLng = destPlace.longitude
))

// → SelectPlaceScreen (출발지 선택)
// → SelectRouteScreen (경로 선택)
// → SaveSelectedRouteUseCase (Room DB 저장)
// → MapScreen으로 복귀
// → selectedRoute가 자동으로 불러와짐
// → Polyline이 지도에 표시됨
```

## 위치 추적 제어

### 시작

```kotlin
// MapRoute에서:
startLocationTracking(
    context = context,
    roomId = roomId,
    userId = userId
)

// 내부 로직:
1. locationRepository.getCurrentLocation()
   → 초기 위치 획득
2. roomRepository.updateMyLocation()
   → Firebase 업데이트
3. locationTrackingManager.startTracking(roomId, userId)
   → Foreground Service 시작
4. locationRepository.observeLocationUpdates()
   → Flow 구독 시작
```

### 중지

```kotlin
// 방 나가기 시:
onAction(MapIntent.LeaveRoom)

stopLocationTracking()

// 내부 로직:
1. locationTrackingManager.stopTracking()
   → Foreground Service 중지
2. LeaveRoomUseCase(roomId, userId)
   → Firebase에서 참가자 제거
```

## 화면 구성

```
MapScreen
├── TopBar
│   ├── Back Button
│   ├── Room Name & Destination
│   └── Invite Button
├── Google Map
│   ├── Participant Markers
│   └── Route Polyline
├── Floating Action Buttons
│   ├── My Location Button
│   └── Route Selection Button
└── ParticipantCardPager
    └── HorizontalPager of ParticipantCard
        ├── Name & Profile Color
        ├── Distance to Destination
        └── Movement Status Icon
```

## 테스트 시나리오

1. **방 참여**
   - 유효한 roomId로 참여 성공
   - 잘못된 roomId로 에러 처리

2. **실시간 위치 동기화**
   - 내 위치 변경 시 즉시 지도 업데이트
   - 다른 참가자 위치 변경 시 마커 업데이트

3. **이동 상태 계산**
   - 목적지에 근접 시 상태 변경 확인
   - 거리 임계값에 따른 상태 변화

4. **경로 선택**
   - 경로 선택 후 Polyline 표시
   - 경로 변경 시 업데이트

5. **방 나가기**
   - 나가기 후 Home 화면 이동
   - Firebase에서 참가자 제거 확인

---

**관련 문서**
- [위치 추적 플로우 상세](LOCATION_FLOW.md)
- [Feature: Home](../home/README.md)
- [Feature: Route](../route/README.md)
- [Sync: Location Service](../../sync/location/README.md)
