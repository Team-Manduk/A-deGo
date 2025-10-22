# Core: Model

애플리케이션 전체에서 사용하는 도메인 모델을 정의하는 모듈입니다.

## 개요

Model 모듈은 A-deGo의 핵심 데이터 구조를 정의합니다. 이 모듈은 순수 Kotlin 데이터 클래스로만 구성되며, 비즈니스 규칙이나 로직은 포함하지 않습니다.

## 주요 모델

### Room (방)

모임 정보를 나타내는 정적 데이터

```kotlin
data class Room(
    val roomId: String,          // 고유 ID (6자리 영숫자)
    val roomName: String,         // 방 이름 (예: "강남역 14시 30분 모임")
    val destination: Place,       // 목적지 정보
    val dateTime: String,         // 모임 시간 (ISO 8601 형식)
    val createdBy: String,        // 생성자 userId
    val createdAt: Long          // 생성 시간 (timestamp)
)
```

**특징**:
- 방 정보는 생성 후 변경되지 않음 (Immutable)
- 참여자 정보는 별도로 실시간 구독

### Participant (참여자)

참여자 정보 및 실시간 상태

```kotlin
data class Participant(
    val userId: String,                      // 고유 ID
    val name: String,                        // 사용자 이름
    val profileColor: String,                // 프로필 색상 (예: "#E53935")
    val location: ParticipantLocation? = null,  // 현재 위치
    val route: ParticipantRoute? = null,     // 선택한 경로
    val distanceToDestination: Int? = null,  // 목적지까지 거리 (미터)
    val movementStatus: MovementStatus       // 이동 상태
)
```

**관련 모델**:

#### ParticipantLocation
```kotlin
data class ParticipantLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,         // 정확도 (미터)
    val timestamp: Long          // 위치 획득 시간
)
```

#### ParticipantRoute
```kotlin
data class ParticipantRoute(
    val startLatitude: Double,
    val startLongitude: Double,
    val totalTime: Int,          // 총 소요 시간 (분)
    val totalDistance: Int,      // 총 거리 (미터)
    val fare: Int,               // 요금 (원)
    val polylineEncoded: String  // 인코딩된 Polyline
)
```

### Place (장소)

장소 정보

```kotlin
data class Place(
    val name: String,            // 장소명 (예: "강남역")
    val address: String,         // 주소
    val latitude: Double,        // 위도
    val longitude: Double,       // 경도
    val isPOI: Boolean = true    // POI 검색 결과 여부
)
```

**isPOI**:
- `true`: 검색 API로 찾은 POI (Point of Interest)
- `false`: 역지오코딩으로 변환된 좌표

### Route (경로)

대중교통 경로 정보

```kotlin
data class Route(
    val totalTime: Int,          // 총 소요 시간 (초)
    val totalDistance: Int,      // 총 거리 (미터)
    val totalFare: Int,          // 총 요금 (원)
    val transferCount: Int,      // 환승 횟수
    val pathType: Int,           // 경로 타입 (1: 지하철, 2: 버스, 3: 혼합)
    val subPaths: List<SubPath>, // 세부 경로 목록
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?
)
```

#### SubPath (세부 경로)

```kotlin
data class SubPath(
    val trafficType: TrafficType,    // SUBWAY, BUS, WALK
    val distance: Double,             // 거리 (미터)
    val sectionTime: Int,             // 소요 시간 (초)

    // 대중교통 정보
    val startName: String?,           // 승차 정류장/역
    val endName: String?,             // 하차 정류장/역
    val stationCount: Int?,           // 정거장 개수
    val lane: Lane?,                  // 노선 정보

    // 좌표 정보
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,

    // 경유지 및 그래픽
    val passStations: List<Station>?,        // 경유 정류장 목록
    val graphicData: List<GraphicCoordinate>?  // 경로 좌표
)
```

#### Lane (노선)

```kotlin
data class Lane(
    val name: String,            // 노선명 (예: "2호선", "360번")
    val busNo: String?,          // 버스 번호
    val type: Int?,              // 버스 타입 (1: 일반, 2: 좌석, ...)
    val subwayCode: Int?         // 지하철 노선 번호
)
```

#### TrafficType (교통 수단)

```kotlin
enum class TrafficType {
    SUBWAY,  // 지하철
    BUS,     // 버스
    WALK     // 도보
}
```

### MovementStatus (이동 상태)

참여자의 현재 이동 상태

```kotlin
enum class MovementStatus(val distanceThresholdMeters: Int?) {
    NOT_STARTED(null),      // 경로 선택 전
    READY(200),             // 출발 전 (출발지 200m 이내)
    IN_PROGRESS(null),      // 이동 중
    ARRIVING_SOON(500),     // 도착 임박 (목적지 500m 이내)
    ARRIVED(100)            // 도착 완료 (목적지 100m 이내)
}
```

**거리 임계값**:
```kotlin
companion object {
    const val ARRIVAL_THRESHOLD = 100         // 도착 판단
    const val ARRIVING_SOON_THRESHOLD = 500   // 도착 임박 판단
    const val READY_THRESHOLD = 200           // 출발 준비 판단
}
```

## 모델 간 관계

```
Room
 ├─ destination: Place
 └─ participants: List<Participant> (별도 관리)

Participant
 ├─ location: ParticipantLocation
 ├─ route: ParticipantRoute
 └─ movementStatus: MovementStatus

Route
 └─ subPaths: List<SubPath>
     └─ lane: Lane
         └─ trafficType: TrafficType
```

## 사용 예시

### Room 생성

```kotlin
val room = Room(
    roomId = "ABC123",
    roomName = "강남역에서 만나요",
    destination = Place(
        name = "강남역",
        address = "서울특별시 강남구 강남대로 396",
        latitude = 37.4979,
        longitude = 127.0276,
        isPOI = true
    ),
    dateTime = "2025-10-25T14:30:00",
    createdBy = "user_123",
    createdAt = System.currentTimeMillis()
)
```

### Participant 상태 업데이트

```kotlin
val participant = Participant(
    userId = "user_456",
    name = "홍길동",
    profileColor = "#E53935",
    location = ParticipantLocation(
        latitude = 37.5145,
        longitude = 127.1027,
        accuracy = 15.0f,
        timestamp = System.currentTimeMillis()
    ),
    route = ParticipantRoute(
        startLatitude = 37.5145,
        startLongitude = 127.1027,
        totalTime = 45,
        totalDistance = 12500,
        fare = 1400,
        polylineEncoded = "encoded_polyline_string"
    ),
    distanceToDestination = 5200,
    movementStatus = MovementStatus.IN_PROGRESS
)
```

### Route 경로 정보

```kotlin
val route = Route(
    totalTime = 2700,        // 45분 (초 단위)
    totalDistance = 12500,   // 12.5km (미터)
    totalFare = 1400,
    transferCount = 1,
    pathType = 1,            // 지하철
    subPaths = listOf(
        SubPath(
            trafficType = TrafficType.WALK,
            distance = 350.0,
            sectionTime = 300  // 5분
        ),
        SubPath(
            trafficType = TrafficType.SUBWAY,
            distance = 10000.0,
            sectionTime = 1800,  // 30분
            startName = "강남역",
            endName = "선릉역",
            stationCount = 5,
            lane = Lane(
                name = "2호선",
                subwayCode = 2
            )
        ),
        SubPath(
            trafficType = TrafficType.WALK,
            distance = 450.0,
            sectionTime = 600  // 10분
        )
    ),
    startLatitude = 37.5145,
    startLongitude = 127.1027,
    endLatitude = 37.4979,
    endLongitude = 127.0276
)
```

### MovementStatus 판단

```kotlin
fun calculateStatus(
    distanceToDestination: Int,
    hasRoute: Boolean
): MovementStatus {
    return when {
        distanceToDestination <= MovementStatus.ARRIVAL_THRESHOLD ->
            MovementStatus.ARRIVED
        distanceToDestination <= MovementStatus.ARRIVING_SOON_THRESHOLD ->
            MovementStatus.ARRIVING_SOON
        hasRoute ->
            MovementStatus.IN_PROGRESS
        else ->
            MovementStatus.NOT_STARTED
    }
}
```

## 데이터 변환

Model은 다른 계층에서 변환됩니다:

### DTO → Model (Data Layer)

```kotlin
// Mapper 예시
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
```

### Model → UiModel (Presentation Layer)

```kotlin
// UI 전용 모델로 변환
fun Participant.toUiModel(destination: Place): ParticipantUiModel {
    return ParticipantUiModel(
        userId = this.userId,
        name = this.name,
        profileColor = this.profileColor,
        location = this.location,
        route = this.route,
        distanceText = formatDistance(this.distanceToDestination),  // "5.2km"
        statusIcon = this.movementStatus.toIcon()
    )
}
```

## 불변성 (Immutability)

모든 모델은 `data class`로 정의되어 불변성을 보장합니다:

```kotlin
// ❌ 직접 수정 불가
participant.name = "새 이름"  // 컴파일 에러

// ✅ copy()로 새 인스턴스 생성
val updated = participant.copy(name = "새 이름")
```

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // 순수 Kotlin - 외부 의존성 없음
}
```

Model 모듈은 **어떤 외부 라이브러리에도 의존하지 않습니다**.

## 테스트

```kotlin
@Test
fun `MovementStatus는 거리에 따라 올바른 상태를 반환한다`() {
    val arrived = MovementStatus.ARRIVED
    assertEquals(100, arrived.distanceThresholdMeters)

    val arrivingSoon = MovementStatus.ARRIVING_SOON
    assertEquals(500, arrivingSoon.distanceThresholdMeters)

    val inProgress = MovementStatus.IN_PROGRESS
    assertNull(inProgress.distanceThresholdMeters)
}

@Test
fun `Participant는 불변 객체다`() {
    val original = Participant(
        userId = "user_1",
        name = "홍길동",
        profileColor = "#E53935",
        movementStatus = MovementStatus.NOT_STARTED
    )

    val updated = original.copy(name = "김철수")

    assertEquals("홍길동", original.name)  // 원본 유지
    assertEquals("김철수", updated.name)   // 새 인스턴스
}
```

## 관련 문서

- [Core: Domain - UseCase & Repository](../domain/README.md)
- [Core: Data - Repository 구현](../data/README.md)

---

**Core Model의 핵심 원칙**:
1. 순수 Kotlin 데이터 클래스
2. 외부 의존성 없음
3. 불변성 보장
4. 비즈니스 로직 배제
