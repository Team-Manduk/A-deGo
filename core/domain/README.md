# Core: Domain

비즈니스 로직과 Repository 인터페이스를 정의하는 Domain Layer 모듈입니다.

## 개요

Domain 모듈은 Clean Architecture의 핵심 계층으로, 비즈니스 로직을 캡슐화한 UseCase와 데이터 접근을 추상화한 Repository 인터페이스를 제공합니다. 이 모듈은 어떤 프레임워크나 외부 라이브러리에도 의존하지 않는 순수 Kotlin 모듈입니다.

## 구조

```
core/domain/
├── repository/          # Repository 인터페이스
│   ├── RoomRepository
│   ├── LocationRepository
│   ├── RouteRepository
│   ├── PlaceRepository
│   ├── UserRepository
│   └── SelectedRouteRepository
├── usecase/            # UseCase 클래스
│   ├── Room 관련
│   ├── Location 관련
│   ├── Route 관련
│   └── Place 관련
└── util/               # Domain 유틸리티
    ├── LocationUtils
    └── PolylineEncoder
```

## Repository 인터페이스

### RoomRepository

방(Room) 및 참가자(Participant) 관리

**주요 메서드**:
```kotlin
interface RoomRepository {
    suspend fun createRoom(roomName: String, destination: Place, dateTime: String, userId: String, userName: String): Result<String>
    suspend fun getRoomInfo(roomId: String): Result<Room?>
    suspend fun joinRoom(roomId: String, userId: String, userName: String): Result<Unit>
    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit>
    fun observeCurrentRoom(roomId: String): Flow<Room?>
    fun observeCurrentParticipants(roomId: String): Flow<List<Participant>>
    suspend fun updateMyLocation(roomId: String, userId: String, location: ParticipantLocation): Result<Unit>
}
```

### LocationRepository

GPS 위치 정보 관리

**주요 메서드**:
```kotlin
interface LocationRepository {
    suspend fun getCurrentLocation(): Result<ParticipantLocation>
    fun observeLocationUpdates(): Flow<ParticipantLocation>
    suspend fun hasLocationPermission(): Boolean
}
```

### RouteRepository

대중교통 경로 검색

**주요 메서드**:
```kotlin
interface RouteRepository {
    suspend fun searchRoutes(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Result<List<Route>>
}
```

### SelectedRouteRepository

선택된 경로 저장 및 조회

**주요 메서드**:
```kotlin
interface SelectedRouteRepository {
    suspend fun saveSelectedRoute(route: Route): Result<Unit>
    fun getSelectedRoute(): Flow<Route?>
    suspend fun clearSelectedRoute()
}
```

### PlaceRepository

장소 검색 및 관리

**주요 메서드**:
```kotlin
interface PlaceRepository {
    fun observeSelectedPlace(): Flow<Place?>
    suspend fun saveSelectedPlace(place: Place)
    suspend fun clearSelectedPlace()
}
```

### UserRepository

사용자 세션 관리

**주요 메서드**:
```kotlin
interface UserRepository {
    suspend fun saveSession(sessionInfo: SessionInfo)
    suspend fun getSession(): SessionInfo?
    suspend fun clearSession()
}
```

## UseCase 클래스

### Room 관련

| UseCase | 설명 |
|---------|------|
| `JoinRoomUseCase` | 방 참여 (방 정보 확인 + 참가자 등록 + 세션 저장) |
| `LeaveRoomUseCase` | 방 나가기 (참가자 제거 + 세션 삭제) |
| `RestoreSessionUseCase` | 앱 재시작 시 세션 복원 |

**예시: JoinRoomUseCase**
```kotlin
class JoinRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(roomId: String, userId: String, userName: String): Result<Unit> {
        // 1. 방 존재 여부 확인
        val roomResult = roomRepository.getRoomInfo(roomId)
        val room = roomResult.getOrNull() ?: return Result.failure(...)

        // 2. 방 참여
        roomRepository.joinRoom(roomId, userId, userName)

        // 3. 세션 저장
        userRepository.saveSession(SessionInfo(roomId, userId, userName))

        return Result.success(Unit)
    }
}
```

### Location 관련

| UseCase | 설명 |
|---------|------|
| `GetCurrentLocationUseCase` | 현재 위치 한 번 조회 |
| `BroadcastLocationUseCase` | 위치를 Firebase로 전송 (LocationTrackingService에서 사용) |
| `UpdateLocationUseCase` | 위치 업데이트 필터링 로직 (25m or 30s) |
| `CalculateMovementStatusUseCase` | 이동 상태 계산 (ARRIVED, ARRIVING_SOON, etc.) |
| `CalculateRouteProgressUseCase` | 경로 진행률 계산 |

**예시: CalculateMovementStatusUseCase**
```kotlin
class CalculateMovementStatusUseCase @Inject constructor() {
    operator fun invoke(
        currentLocation: ParticipantLocation?,
        startLocation: ParticipantLocation?,
        destination: Place?,
        hasRoute: Boolean
    ): MovementStatus {
        if (currentLocation == null || destination == null) {
            return MovementStatus.NOT_STARTED
        }

        val distanceToDestination = LocationUtils.calculateDistance(
            currentLocation.latitude, currentLocation.longitude,
            destination.latitude, destination.longitude
        )

        return when {
            distanceToDestination <= MovementStatus.ARRIVED.distanceThreshold -> MovementStatus.ARRIVED
            distanceToDestination <= MovementStatus.ARRIVING_SOON.distanceThreshold -> MovementStatus.ARRIVING_SOON
            hasRoute -> MovementStatus.IN_PROGRESS
            startLocation != null && LocationUtils.calculateDistance(...) <= MovementStatus.READY.distanceThreshold -> MovementStatus.READY
            else -> MovementStatus.NOT_STARTED
        }
    }
}
```

### Route 관련

| UseCase | 설명 |
|---------|------|
| `SearchRouteUseCase` | TMAP API로 경로 검색 |
| `SaveSelectedRouteUseCase` | 선택된 경로를 Room DB + Firebase에 저장 |

**예시: SaveSelectedRouteUseCase**
```kotlin
class SaveSelectedRouteUseCase @Inject constructor(
    private val selectedRouteRepository: SelectedRouteRepository,
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(
        route: Route,
        roomId: String,
        userId: String
    ): Result<Unit> {
        // 1. 로컬 DB 저장
        selectedRouteRepository.saveSelectedRoute(route)

        // 2. Firebase 동기화
        roomRepository.updateParticipantRoute(roomId, userId, route)

        return Result.success(Unit)
    }
}
```

### Place 관련

| UseCase | 설명 |
|---------|------|
| `GetSelectedPlaceUseCase` | 선택된 장소 Flow로 관찰 |
| `SetSelectedPlaceUseCase` (SaveSelectedPlaceUseCase) | 장소 선택 저장 |
| `ClearSelectedPlaceUseCase` | 장소 선택 초기화 |
| `SearchPlaceByTextUseCase` | 텍스트로 장소 검색 (향후 구현) |
| `SearchPlaceByCoordinatesUseCase` | 좌표로 장소 검색 (역지오코딩) |

## Domain Utilities

### LocationUtils

위치 관련 유틸리티 함수

**주요 메서드**:
```kotlin
object LocationUtils {
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    // Haversine formula를 사용한 거리 계산 (미터 단위)
}
```

### PolylineEncoder

Google Polyline 인코딩/디코딩

**주요 메서드**:
```kotlin
object PolylineEncoder {
    fun encode(coordinates: List<Coordinate>): String
    fun decode(encoded: String): List<Coordinate>
}
```

TMAP API에서 받은 경로 좌표를 압축된 문자열로 인코딩하여 Firebase에 저장합니다.

## UseCase 명명 규칙

- **동사형 네이밍**: `JoinRoom`, `SearchRoute`, `CalculateMovementStatus`
- **단일 책임**: 각 UseCase는 하나의 비즈니스 로직만 담당
- **operator fun invoke()**: 함수형 호출 지원

```kotlin
// 사용 예시:
val joinRoom = JoinRoomUseCase(...)
joinRoom(roomId, userId, userName)  // invoke() 호출
```

## 의존성 규칙

Domain Layer는 **의존성의 최상위**에 위치합니다:

```
Domain Layer (순수 Kotlin)
    ↑
    │ (의존)
    │
Data Layer (Repository 구현체)
    ↑
    │
Presentation Layer (ViewModel, UI)
```

**Domain은 다른 레이어에 의존하지 않습니다**:
- ❌ Android Framework 의존 금지
- ❌ Firebase, Ktor 등 외부 라이브러리 의존 금지
- ✅ 순수 Kotlin + Coroutines만 사용

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":core:model"))        // Domain 모델
    implementation(libs.kotlinx.coroutines.core)  // Flow, suspend
    implementation(libs.javax.inject)             // @Inject annotation
}
```

## 테스트

Domain Layer는 외부 의존성이 없어 **단위 테스트가 가장 쉬운 계층**입니다:

```kotlin
@Test
fun `목적지에 도착하면 ARRIVED 상태를 반환한다`() {
    val useCase = CalculateMovementStatusUseCase()

    val currentLocation = ParticipantLocation(37.4979, 127.0276, ...)
    val destination = Place(latitude = 37.4980, longitude = 127.0276, ...)

    val status = useCase(currentLocation, null, destination, true)

    assertEquals(MovementStatus.ARRIVED, status)
}
```

## 주요 패턴

### 1. Repository Pattern

데이터 소스를 추상화하여 비즈니스 로직에서 데이터 출처를 숨깁니다.

### 2. UseCase Pattern

비즈니스 로직을 독립적인 클래스로 분리하여 재사용성과 테스트 용이성을 높입니다.

### 3. Result Pattern

Kotlin의 `Result<T>` 타입으로 성공/실패를 명확하게 표현합니다.

```kotlin
suspend fun getRoomInfo(roomId: String): Result<Room?>

// 사용:
roomRepository.getRoomInfo(roomId).fold(
    onSuccess = { room -> /* 성공 처리 */ },
    onFailure = { exception -> /* 에러 처리 */ }
)
```

## 관련 문서

- [Core: Data - Repository 구현](../data/README.md)
- [Core: Model - Domain 모델](../model/README.md)
- [Feature 모듈들](../../feature)

---

**Domain Layer의 핵심 원칙**:
1. 순수 비즈니스 로직만 포함
2. 외부 의존성 최소화
3. 테스트 가능한 코드 작성
