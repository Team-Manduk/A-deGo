# Feature: Place

Google Maps와 Places API를 사용한 장소 선택 기능을 제공하는 Feature 모듈입니다.

## 개요

Place 모듈은 사용자가 지도에서 장소를 검색하고 선택할 수 있는 UI를 제공합니다. Create(목적지 선택) 및 Map(출발지 선택) 화면에서 공통으로 사용됩니다.

## 주요 기능

### 1. Google Maps 통합

- **Google Maps Compose**: 인터랙티브 지도 렌더링
- **마커**: 선택된 위치 표시
- **카메라 제어**: 선택된 위치로 자동 이동

### 2. 장소 검색 (향후 구현 예정)

- Google Places Autocomplete
- 검색 결과 표시
- 검색 기록

### 3. 지도 클릭 선택

- 지도 터치 시 마커 이동
- 선택된 좌표 표시
- "선택 완료" 버튼

### 4. 컨텍스트별 네비게이션

- **Create에서 호출**: 목적지 선택 → CreateScreen 복귀
- **Map에서 호출**: 출발지 선택 → SelectRouteScreen으로 이동

## 구조

### Components

#### SelectPlaceRoute (Composable)
- ViewModel 상태 관찰
- 네비게이션 처리

#### SelectPlaceScreen (Composable)
- Google Maps 렌더링
- 검색바 (TODO)
- "선택 완료" 버튼

#### SelectPlaceViewModel
- 선택된 장소 상태 관리
- SaveSelectedPlaceUseCase 호출

## 데이터 플로우

### Create에서 장소 선택 플로우

```
CreateScreen
    ↓
User clicks "장소 선택" field
    ↓
navigateToSelectPlace(mode = PlaceSelectionMode.DESTINATION)
    ↓
SelectPlaceScreen
    ↓
User clicks on map
    ↓
onMapClick { latLng ->
    viewModel.updateSelectedLocation(latLng)
    selectedLocation = latLng
}
    ↓
User clicks "선택 완료"
    ↓
viewModel.confirmSelection(
    mode = DESTINATION,
    roomId = null,
    userId = null,
    destLat = null,
    destLng = null
)
    ↓
SaveSelectedPlaceUseCase(Place(...))
    ↓
placeRepository.saveSelectedPlace(place)
    ↓
UserDataStore에 저장
    ↓
navigateBack()
    ↓
CreateScreen (자동으로 GetSelectedPlaceUseCase로 불러옴)
```

### Map에서 출발지 선택 플로우

```
MapScreen
    ↓
User clicks "경로 선택" button
    ↓
navigateToSelectStartPlace(
    mode = PlaceSelectionMode.START,
    roomId = roomId,
    userId = userId,
    destLat = destination.latitude,
    destLng = destination.longitude
)
    ↓
SelectPlaceScreen (출발지 모드)
    ↓
User selects start location
    ↓
User clicks "선택 완료"
    ↓
viewModel.confirmSelection(
    mode = START,
    roomId = roomId,
    userId = userId,
    destLat = destLat,
    destLng = destLng
)
    ↓
navigateToSelectRoute(
    roomId, userId,
    startLat, startLng,
    destLat, destLng
)
    ↓
SelectRouteScreen
```

## UI State

### SelectPlaceUiState

```kotlin
data class SelectPlaceUiState(
    val selectedLocation: LatLng? = null,    // 선택된 좌표
    val searchQuery: String = "",            // 검색어 (향후 사용)
    val isLoading: Boolean = false,          // 로딩 상태
    val error: String? = null                // 에러 메시지
)
```

## Dependencies

### Domain Layer
```kotlin
- SaveSelectedPlaceUseCase      // 선택된 장소 저장
```

### Core Modules
```kotlin
- core:model                    // Place
- core:navigation               // Route.SelectPlace
```

### Google Libraries
```kotlin
- com.google.maps.android:maps-compose  // Google Maps Compose
- com.google.android.gms:play-services-maps  // Maps SDK
```

## 주요 클래스

### SelectPlaceViewModel

**파일 위치**: `feature/place/src/main/kotlin/com/teammanduk/adego/feature/place/SelectPlaceViewModel.kt`

**주요 메서드**:
- `updateSelectedLocation(latLng: LatLng)`: 선택된 위치 업데이트
- `confirmSelection(...)`: 장소 선택 확정 및 네비게이션

### PlaceSelectionMode

```kotlin
enum class PlaceSelectionMode {
    DESTINATION,    // 목적지 선택 (Create에서 호출)
    START           // 출발지 선택 (Map에서 호출)
}
```

## 사용 예시

### Create에서 목적지 선택

```kotlin
// CreateScreen에서:
onNavigateToSelectPlace = {
    navigator.navigateToSelectPlace()
}

// SelectPlaceNavigation:
fun NavGraphBuilder.placeNavGraph(
    onNavigateBack: () -> Unit,
    onStartPlaceSelected: (String, String, Double, Double, Double, Double) -> Unit
) {
    composable<Route.SelectPlace> { backStackEntry ->
        val args = backStackEntry.toRoute<Route.SelectPlace>()

        SelectPlaceRoute(
            mode = args.mode ?: PlaceSelectionMode.DESTINATION.name,
            onNavigateBack = onNavigateBack,
            onStartPlaceSelected = onStartPlaceSelected
        )
    }
}

// SelectPlaceScreen에서:
GoogleMap(
    onMapClick = { latLng ->
        selectedLocation = latLng
    }
)

Button(onClick = {
    viewModel.confirmSelection(
        mode = PlaceSelectionMode.DESTINATION,
        // roomId, userId, destLat, destLng는 null
    )
})

// → SaveSelectedPlaceUseCase 저장
// → navigateBack()
// → CreateScreen에서 GetSelectedPlaceUseCase로 자동 로드
```

## 개선 가능 사항

현재 구현:
- 지도 클릭으로만 선택 가능
- 좌표만 저장 (주소 정보 없음)

향후 개선:
- [ ] Google Places Autocomplete 통합
- [ ] 역지오코딩 (좌표 → 주소)
- [ ] 최근 검색 위치 저장
- [ ] 즐겨찾기 위치
- [ ] 현재 위치 버튼

---

**관련 문서**
- [Feature: Create](../create/README.md)
- [Feature: Map](../map/README.md)
- [Feature: Route](../route/README.md)
