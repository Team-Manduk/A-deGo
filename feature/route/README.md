# Feature: Route

TMAP API를 사용한 대중교통 경로 검색 및 선택 기능을 제공하는 Feature 모듈입니다.

## 개요

Route 모듈은 출발지와 목적지 간의 대중교통 경로를 검색하고, 여러 옵션 중 하나를 선택할 수 있는 UI를 제공합니다. 한국의 지하철, 버스, 도보를 조합한 최적 경로를 제안합니다.

## 주요 기능

### 1. TMAP 경로 검색

- **SearchRouteUseCase**: TMAP API 호출
- **다중 경로 제공**: 빠른 경로, 최소 환승, 최소 도보 등
- **상세 정보**:
  - 총 소요 시간
  - 총 거리
  - 요금
  - 세부 경로 (SubPath)

### 2. 경로 선택 UI

- **경로 카드**: 각 경로 옵션을 카드로 표시
- **상세 정보 확장**: 세부 경로 단계별 표시
- **교통수단 아이콘**: 지하철, 버스, 도보 등 시각화

### 3. 경로 저장

- **Room Database**: 선택된 경로를 로컬에 저장
- **Firebase 동기화**: 다른 참가자도 내 경로 확인 가능

## 구조

### Components

#### SelectRouteRoute (Composable)
- ViewModel 상태 관찰
- 경로 검색 트리거
- 경로 선택 처리

#### SelectRouteScreen (Composable)
- 경로 목록 표시
- 로딩 및 에러 상태 처리

#### SelectRouteViewModel
- 경로 검색 로직
- 경로 선택 및 저장

## 데이터 플로우

### 경로 검색 및 선택 플로우

```
MapScreen
    ↓
User clicks "경로 선택"
    ↓
navigateToSelectStartPlace(roomId, userId, destLat, destLng)
    ↓
SelectPlaceScreen (출발지 선택)
    ↓
User selects start location
    ↓
navigateToSelectRoute(roomId, userId, startLat, startLng, destLat, destLng)
    ↓
SelectRouteScreen
    ↓
SelectRouteViewModel.init
    ↓
SearchRouteUseCase.invoke(startLat, startLng, destLat, destLng)
    ↓
routeRepository.searchRoutes()
    ↓
placeDataSource.getTmapRoutes()
    ↓
TMAP API: POST /transit/routes
    ↓
Response: List<RouteDto>
    ↓
Mapper: RouteDto → Route
    ↓
_uiState.update { routes = routes, isLoading = false }
    ↓
SelectRouteScreen displays route cards
    ↓
User clicks on a route card
    ↓
viewModel.selectRoute(selectedRoute)
    ↓
SaveSelectedRouteUseCase.invoke(route)
    ↓
selectedRouteRepository.saveSelectedRoute(route)
    ↓
Room DB: SelectedRouteEntity 저장
Firebase: participants/{roomId}/{userId}/route 업데이트
    ↓
navigateBack() → MapScreen
    ↓
MapScreen auto-loads selected route
    ↓
Polyline displayed on map
```

## UI State

### SelectRouteUiState

```kotlin
data class SelectRouteUiState(
    val routes: List<Route> = emptyList(),    // 검색된 경로 목록
    val isLoading: Boolean = false,           // 로딩 상태
    val error: String? = null                 // 에러 메시지
)
```

## Route 모델

### Route (Domain Model)

```kotlin
data class Route(
    val totalTime: Int,              // 총 소요 시간 (분)
    val totalDistance: Int,          // 총 거리 (미터)
    val totalWalkTime: Int,          // 도보 시간 (분)
    val totalWalkDistance: Int,      // 도보 거리 (미터)
    val fare: Int,                   // 요금 (원)
    val subPaths: List<SubPath>,     // 세부 경로
    val graphCoordinates: List<Coordinate>  // Polyline 좌표
)
```

### SubPath (세부 경로)

```kotlin
data class SubPath(
    val trafficType: TrafficType,    // 교통수단 (지하철, 버스, 도보)
    val distance: Int,                // 거리
    val sectionTime: Int,             // 소요 시간
    val startName: String?,           // 출발 정류장/역
    val endName: String?,             // 도착 정류장/역
    val lane: Lane?                   // 노선 정보 (지하철선, 버스번호 등)
)
```

### TrafficType

```kotlin
enum class TrafficType {
    SUBWAY,     // 지하철
    BUS,        // 버스
    WALK,       // 도보
    UNKNOWN
}
```

## Dependencies

### Domain Layer
```kotlin
- SearchRouteUseCase            // TMAP 경로 검색
- SaveSelectedRouteUseCase      // 선택된 경로 저장
```

### Data Layer
```kotlin
- RouteRepository               // 경로 검색 및 저장
- SelectedRouteRepository       // 선택된 경로 관리
```

### Remote
```kotlin
- PlaceDataSource (TMAP API)    // 경로 검색 API 호출
```

## 주요 클래스

### SelectRouteViewModel

**파일 위치**: `feature/route/src/main/kotlin/com/teammanduk/adego/feature/route/SelectRouteViewModel.kt`

**주요 메서드**:
- `searchRoutes(startLat, startLng, destLat, destLng)`: 경로 검색
- `selectRoute(route: Route)`: 경로 선택 및 저장

## TMAP API 통합

### API Endpoint

```
POST https://apis.openapi.sk.com/transit/routes
```

### Request Body

```json
{
  "startX": 127.0276,
  "startY": 37.4979,
  "endX": 127.1027,
  "endY": 37.5145,
  "format": "json",
  "count": 5
}
```

### Response

```json
{
  "metaData": {
    "plan": {
      "itineraries": [
        {
          "totalTime": 45,
          "totalDistance": 12500,
          "totalWalkTime": 10,
          "totalWalkDistance": 800,
          "fare": {
            "regular": {
              "totalFare": 1400
            }
          },
          "legs": [
            {
              "mode": "SUBWAY",
              "sectionTime": 30,
              "distance": 10000,
              "start": {
                "name": "강남역"
              },
              "end": {
                "name": "선릉역"
              },
              "route": "2호선"
            }
          ]
        }
      ]
    }
  }
}
```

## 경로 카드 UI

```
RouteCard
├── Header
│   ├── 총 소요 시간: "45분"
│   └── 요금: "1,400원"
├── SubPath Icons (가로 스크롤)
│   ├── 🚶 도보 5분
│   ├── 🚇 2호선 (강남역 → 선릉역)
│   └── 🚶 도보 5분
└── Details (펼치기/접기)
    ├── 총 거리: "12.5km"
    ├── 도보 거리: "800m"
    └── 세부 경로 단계별 표시
```

## 사용 예시

### 경로 검색 및 선택

```kotlin
// MapScreen에서:
"경로 선택" 버튼 클릭
    ↓
navigateToSelectStartPlace(
    roomId = "ABC123",
    userId = "user_123",
    destLat = 37.4979,
    destLng = 127.0276
)

// SelectPlaceScreen에서:
출발지 선택 (예: 선릉역)
    ↓
navigateToSelectRoute(
    roomId = "ABC123",
    userId = "user_123",
    startLat = 37.5145,
    startLng = 127.1027,
    destLat = 37.4979,
    destLng = 127.0276
)

// SelectRouteViewModel.init:
searchRoutes(37.5145, 127.1027, 37.4979, 127.0276)
    ↓
TMAP API 호출
    ↓
routes = [
    Route(totalTime=45, fare=1400, ...),
    Route(totalTime=50, fare=1250, ...),  // 환승 적음
    Route(totalTime=48, fare=1400, ...)   // 도보 적음
]

// 사용자가 첫 번째 경로 선택:
selectRoute(routes[0])
    ↓
SaveSelectedRouteUseCase(routes[0])
    ↓
Room DB 저장
Firebase 동기화
    ↓
navigateBack() → MapScreen
    ↓
MapScreen에서 자동으로 Polyline 표시
```

## 에러 처리

```kotlin
// TMAP API 에러
catch (e: Exception) {
    _uiState.update {
        it.copy(
            isLoading = false,
            error = "경로를 찾을 수 없습니다: ${e.message}"
        )
    }
}

// UI:
error?.let { errorMessage ->
    Text(
        text = errorMessage,
        color = Color.Red
    )
}
```

## 테스트 시나리오

1. **경로 검색 성공**
   - TMAP API 정상 응답
   - 다중 경로 표시

2. **경로 선택**
   - 경로 선택 후 Room DB 저장 확인
   - MapScreen에서 Polyline 표시 확인

3. **에러 처리**
   - 네트워크 에러 시 에러 메시지 표시
   - 경로 없음 (도서 지역 등)

4. **로딩 상태**
   - API 호출 중 로딩 표시
   - 완료 후 로딩 제거

## 개선 가능 사항

- [ ] 경로 미리보기 (지도에 Polyline 표시)
- [ ] 경로 필터링 (빠른 경로, 최소 환승, 최소 도보)
- [ ] 실시간 교통 정보 반영
- [ ] 즐겨찾기 경로 저장

---

**관련 문서**
- [Feature: Map](../map/README.md)
- [Feature: Place](../place/README.md)
- [Core: Domain - Route Management](../../core/domain/README.md)
