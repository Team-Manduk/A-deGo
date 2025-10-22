# Core: Data

Repository 구현체 및 데이터 매핑을 담당하는 Data Layer 모듈입니다.

## 개요

Data 모듈은 Domain Layer의 Repository 인터페이스를 구현하고, 다양한 데이터 소스(Firebase, Room DB, DataStore)로부터 데이터를 가져와 Domain 모델로 변환합니다.

## 구조

```
core/data/
├── repository/              # Repository 구현체
│   ├── RoomRepositoryImpl
│   ├── LocationRepositoryImpl
│   ├── RouteRepositoryImpl
│   ├── SelectedRouteRepositoryImpl
│   ├── PlaceRepositoryImpl
│   └── UserRepositoryImpl
├── mapper/                  # DTO ↔ Model 변환
│   ├── RoomMapper
│   ├── RouteMapper
│   └── PlaceMapper
├── model/                   # Data Layer 전용 모델
│   └── TmapModels
└── di/                      # Dependency Injection
    └── DataModule
```

## Repository 구현체

### RoomRepositoryImpl

방 및 참가자 관리 구현체

**데이터 소스**:
- `FirebaseRoomDataSource` - Firebase Realtime Database

**주요 기능**:
```kotlin
class RoomRepositoryImpl @Inject constructor(
    private val firebaseRoomDataSource: RoomDataSource
) : RoomRepository {
    // 방 생성
    override suspend fun createRoom(...): Result<String>

    // 방 참여
    override suspend fun joinRoom(...): Result<Unit>

    // 실시간 방 정보 구독
    override fun observeCurrentRoom(roomId: String): Flow<Room?>

    // 실시간 참가자 목록 구독
    override fun observeCurrentParticipants(roomId: String): Flow<List<Participant>>

    // 내 위치 업데이트
    override suspend fun updateMyLocation(...): Result<Unit>
}
```

**특징**:
- Firebase ValueEventListener를 Flow로 변환
- DTO를 Domain Model로 자동 매핑
- 에러를 Result로 래핑

### LocationRepositoryImpl

GPS 위치 정보 관리 구현체

**데이터 소스**:
- `FusedLocationDataSource` - Google Play Services Location

**주요 기능**:
```kotlin
class LocationRepositoryImpl @Inject constructor(
    private val fusedLocationDataSource: LocationDataSource,
    private val context: Context
) : LocationRepository {
    // 현재 위치 한 번 조회
    override suspend fun getCurrentLocation(): Result<ParticipantLocation>

    // 실시간 위치 업데이트 구독
    override fun observeLocationUpdates(): Flow<ParticipantLocation>

    // 권한 확인
    override suspend fun hasLocationPermission(): Boolean
}
```

**Flow 변환**:
```kotlin
override fun observeLocationUpdates(): Flow<ParticipantLocation> {
    return fusedLocationDataSource.getLocationUpdates()
        .map { locationDto -> locationDto.toModel() }
}
```

### RouteRepositoryImpl

경로 검색 구현체

**데이터 소스**:
- `PlaceDataSource` - TMAP API

**주요 기능**:
```kotlin
class RouteRepositoryImpl @Inject constructor(
    private val placeDataSource: PlaceDataSource
) : RouteRepository {
    override suspend fun searchRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Result<List<Route>> {
        return try {
            val routes = placeDataSource.getTmapRoutes(startLat, startLng, endLat, endLng)
            Result.success(routes.map { it.toModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### SelectedRouteRepositoryImpl

선택된 경로 저장/조회 구현체

**데이터 소스**:
- `SelectedRouteDataSource` - Room Database

**주요 기능**:
```kotlin
class SelectedRouteRepositoryImpl @Inject constructor(
    private val selectedRouteDataSource: SelectedRouteDataSource
) : SelectedRouteRepository {
    // 경로 저장 (Room DB)
    override suspend fun saveSelectedRoute(route: Route): Result<Unit>

    // 저장된 경로 관찰 (Flow)
    override fun getSelectedRoute(): Flow<Route?>

    // 경로 삭제
    override suspend fun clearSelectedRoute()
}
```

### PlaceRepositoryImpl

장소 선택 상태 관리 구현체

**데이터 소스**:
- `UserDataSource` - DataStore Preferences

**주요 기능**:
```kotlin
class PlaceRepositoryImpl @Inject constructor(
    private val userDataSource: UserDataSource
) : PlaceRepository {
    // 선택된 장소 관찰
    override fun observeSelectedPlace(): Flow<Place?>

    // 장소 저장
    override suspend fun saveSelectedPlace(place: Place)

    // 장소 초기화
    override suspend fun clearSelectedPlace()
}
```

### UserRepositoryImpl

사용자 세션 관리 구현체

**데이터 소스**:
- `SessionDataSource` - DataStore Preferences

**주요 기능**:
```kotlin
class UserRepositoryImpl @Inject constructor(
    private val sessionDataSource: SessionDataSource
) : UserRepository {
    override suspend fun saveSession(sessionInfo: SessionInfo)

    override suspend fun getSession(): SessionInfo?

    override suspend fun clearSession()
}
```

## Mapper (매퍼)

### RoomMapper

Firebase DTO ↔ Domain Model 변환

```kotlin
// ParticipantDto → Participant
fun ParticipantDto.toModel(): Participant {
    return Participant(
        userId = this.userId,
        name = this.name,
        profileColor = this.profileColor,
        location = this.location?.toModel(),
        route = this.route?.toModel(),
        distanceToDestination = this.distanceToDestination,
        movementStatus = this.movementStatus
    )
}

// Participant → ParticipantDto
fun Participant.toDto(): ParticipantDto {
    return ParticipantDto(
        userId = this.userId,
        name = this.name,
        profileColor = this.profileColor,
        location = this.location?.toDto(),
        route = this.route?.toDto(),
        distanceToDestination = this.distanceToDestination,
        movementStatus = this.movementStatus
    )
}
```

### RouteMapper

TMAP API 응답 → Domain Model 변환

```kotlin
fun RouteDto.toModel(): Route {
    return Route(
        totalTime = this.info.totalTime,
        totalDistance = this.info.totalDistance,
        totalFare = this.info.fare.regular.totalFare,
        transferCount = this.info.transferCount,
        pathType = this.info.pathType,
        subPaths = this.legs.map { it.toSubPath() },
        startLatitude = this.legs.firstOrNull()?.start?.lat,
        startLongitude = this.legs.firstOrNull()?.start?.lon,
        endLatitude = this.legs.lastOrNull()?.end?.lat,
        endLongitude = this.legs.lastOrNull()?.end?.lon
    )
}
```

**복잡한 중첩 구조 변환**:
```kotlin
private fun LegDto.toSubPath(): SubPath {
    return SubPath(
        trafficType = when (mode) {
            "SUBWAY" -> TrafficType.SUBWAY
            "BUS" -> TrafficType.BUS
            "WALK" -> TrafficType.WALK
            else -> TrafficType.WALK
        },
        distance = distance,
        sectionTime = sectionTime,
        startName = start.name,
        endName = end.name,
        stationCount = stationCount,
        lane = route?.toLane(),
        // ... 추가 필드 매핑
    )
}
```

### PlaceMapper

Place 관련 변환 (향후 확장)

```kotlin
fun PlaceDto.toModel(): Place {
    return Place(
        name = this.name,
        address = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        isPOI = this.isPOI
    )
}
```

## 데이터 플로우

### Firebase 실시간 구독 예시

```
Firebase ValueEventListener (콜백)
    ↓
callbackFlow { ... }
    ↓
Flow<ParticipantDto>
    ↓
.map { it.toModel() }
    ↓
Flow<Participant>
    ↓
ViewModel collects
```

**구현 코드**:
```kotlin
override fun observeCurrentParticipants(roomId: String): Flow<List<Participant>> {
    return firebaseRoomDataSource.observeParticipants(roomId)
        .map { participantDtos ->
            participantDtos.map { it.toModel() }
        }
}
```

### Room DB Flow 예시

```
Room DAO Query
    ↓
Flow<SelectedRouteEntity?>
    ↓
.map { it?.toModel() }
    ↓
Flow<Route?>
    ↓
ViewModel collects
```

**구현 코드**:
```kotlin
override fun getSelectedRoute(): Flow<Route?> {
    return selectedRouteDataSource.observeSelectedRoute()
        .map { entity -> entity?.toModel() }
}
```

## Dependency Injection

### DataModule

Hilt를 사용한 Repository 바인딩

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindRoomRepository(
        impl: RoomRepositoryImpl
    ): RoomRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository

    // ... 나머지 Repository 바인딩
}
```

**특징**:
- `@Binds`로 인터페이스 ↔ 구현체 연결
- `@Singleton`으로 앱 전역 단일 인스턴스 보장
- `SingletonComponent`에 설치하여 앱 생명주기와 동일

## 에러 처리

### Result 패턴 사용

```kotlin
override suspend fun createRoom(...): Result<String> {
    return try {
        val roomId = firebaseRoomDataSource.createRoom(...)
        Result.success(roomId)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Flow 에러 처리

```kotlin
override fun observeCurrentRoom(roomId: String): Flow<Room?> {
    return firebaseRoomDataSource.observeRoom(roomId)
        .map { roomDto -> roomDto?.toModel() }
        .catch { e ->
            // 에러 로깅
            Log.e(TAG, "Error observing room", e)
            emit(null)  // 에러 시 null 방출
        }
}
```

## 테스트

### Repository 테스트 (Mock DataSource)

```kotlin
@Test
fun `방 참여 성공 시 Result success를 반환한다`() = runTest {
    // Given
    val mockDataSource = mockk<RoomDataSource>()
    coEvery {
        mockDataSource.joinRoom(any(), any(), any())
    } returns Unit

    val repository = RoomRepositoryImpl(mockDataSource)

    // When
    val result = repository.joinRoom("ABC123", "user_1", "홍길동")

    // Then
    assertTrue(result.isSuccess)
    coVerify { mockDataSource.joinRoom("ABC123", "user_1", "홍길동") }
}
```

### Mapper 테스트

```kotlin
@Test
fun `ParticipantDto는 Participant로 올바르게 변환된다`() {
    // Given
    val dto = ParticipantDto(
        userId = "user_1",
        name = "홍길동",
        profileColor = "#E53935",
        location = null,
        route = null,
        distanceToDestination = null,
        movementStatus = MovementStatus.NOT_STARTED
    )

    // When
    val model = dto.toModel()

    // Then
    assertEquals("user_1", model.userId)
    assertEquals("홍길동", model.name)
    assertEquals("#E53935", model.profileColor)
    assertEquals(MovementStatus.NOT_STARTED, model.movementStatus)
}
```

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":core:domain"))      // Repository 인터페이스
    implementation(project(":core:model"))       // Domain 모델
    implementation(project(":core:data-api"))    // DataSource 인터페이스
    implementation(project(":core:local"))       // Room, DataStore 구현
    implementation(project(":core:remote"))      // Firebase, API 구현

    implementation(libs.hilt.android)            // DI
    implementation(libs.kotlinx.coroutines.core) // Flow
}
```

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────┐
│         Domain Layer                    │
│  - Repository Interface                 │
│  - Domain Models                        │
└─────────────────────────────────────────┘
                 ↑
                 │ implements
                 │
┌─────────────────────────────────────────┐
│         Data Layer (이 모듈)            │
│  ┌─────────────────────────────────┐   │
│  │  Repository Implementations     │   │
│  │  - RoomRepositoryImpl           │   │
│  │  - LocationRepositoryImpl       │   │
│  │  - RouteRepositoryImpl          │   │
│  └─────────────────────────────────┘   │
│                ↓                        │
│  ┌─────────────────────────────────┐   │
│  │  Mappers                        │   │
│  │  - DTO → Model                  │   │
│  │  - Model → DTO                  │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│     DataSource Layer                    │
│  - FirebaseRoomDataSource (Remote)      │
│  - FusedLocationDataSource (Remote)     │
│  - SelectedRouteDataSource (Local)      │
│  - SessionDataSource (Local)            │
└─────────────────────────────────────────┘
```

## 관련 문서

- [Core: Domain - Repository 인터페이스](../domain/README.md)
- [Core: Model - Domain 모델](../model/README.md)
- [Core: Remote - Firebase & API](../remote/README.md)
- [Core: Local - Room DB & DataStore](../local/README.md)

---

**Core Data의 핵심 원칙**:
1. Repository 패턴으로 데이터 소스 추상화
2. Mapper로 계층 간 데이터 변환
3. Result 패턴으로 명확한 에러 처리
4. Flow로 리액티브 데이터 스트림 제공
