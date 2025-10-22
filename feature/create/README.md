# Feature: Create

모임 생성을 담당하는 Feature 모듈입니다.

## 개요

Create 모듈은 사용자가 새로운 모임을 만드는 전체 플로우를 관리합니다. 단계별 UI를 통해 장소, 시간, 방 이름을 설정하고 Firebase에 방을 생성합니다.

## 주요 기능

### 1. 단계별 방 생성 플로우

Create 화면은 **점진적 공개(Progressive Disclosure)** 패턴을 사용합니다:

```
CreateStep.PLACE_SELECTION     → 장소 선택
    ↓
CreateStep.TIME_SELECTION      → 날짜 및 시간 선택
    ↓
CreateStep.ROOM_NAME_INPUT     → 방 이름 입력
    ↓
CreateStep.READY_TO_CREATE     → "모임 생성하기" 버튼 표시
```

각 단계를 완료하면 다음 단계 UI가 자동으로 나타납니다.

### 2. 장소 선택

- SelectPlace 화면으로 네비게이션
- Google Maps/Places API를 통한 장소 검색
- 선택된 장소 정보 자동 입력
- 모임 장소 이름 커스터마이징 (최대 30자)

### 3. 시간 선택

- **날짜 선택**: Material 3 DatePicker
- **시간 선택**: Material 3 TimePicker
- 선택된 시간이 타이틀에 표시 (예: "14시 30분 모임 시작!")

### 4. 자동 방 이름 생성

장소명과 시간을 조합하여 방 이름을 자동 생성:

```kotlin
// 예시:
placeName = "강남역"
time = "14시 30분"
→ roomName = "강남역 14시 30분 모임"
```

사용자가 원하면 직접 수정 가능 (최대 20자)

### 5. Firebase에 방 생성

모든 정보 입력 완료 후 "모임 생성하기" 버튼 활성화:

- 고유한 roomId 생성 (6자리 영숫자)
- Firebase Realtime Database에 방 정보 저장
- 생성자를 첫 번째 참가자로 자동 등록
- 생성 완료 후 Map 화면으로 자동 이동

## 구조

### Components

#### CreateRoute (Composable)
- ViewModel 상태 및 SideEffect 처리
- 에러 다이얼로그 표시
- 네비게이션 처리

#### CreateScreen (Composable)
- 단계별 UI 렌더링
- DatePicker, TimePicker 모달 관리
- 3개의 섹션 컴포넌트로 구성

#### CreateViewModel
- MVI 패턴 구현
- 단계 자동 계산
- 방 생성 로직

## 데이터 플로우

### 방 생성 플로우

```
1. 장소 선택
   User clicks "장소 선택" field
   ↓
   CreateIntent.NavigateToSelectPlace
   ↓
   SelectPlaceScreen (feature:place)
   ↓
   User selects place
   ↓
   GetSelectedPlaceUseCase() emits Place
   ↓
   CreateIntent.SelectPlace(place)
   ↓
   uiState.selectedPlace = place
   uiState.meetingPlaceName = place.name
   uiState.currentStep = TIME_SELECTION ← 다음 단계 표시

2. 시간 선택
   User clicks "날짜 선택"
   ↓
   DatePicker shows
   ↓
   CreateIntent.SelectDate(dateMillis)
   ↓
   uiState.selectedDate = dateMillis

   User clicks "시간 선택"
   ↓
   TimePicker shows
   ↓
   CreateIntent.SelectTime(hour, minute)
   ↓
   uiState.selectedHour = hour
   uiState.selectedMinute = minute
   uiState.roomName = auto-generated ← "강남역 14시 30분 모임"
   uiState.currentStep = ROOM_NAME_INPUT ← 다음 단계 표시

3. 방 이름 입력 (optional)
   User edits roomName
   ↓
   CreateIntent.UpdateRoomName(name)
   ↓
   uiState.roomName = name
   uiState.currentStep = READY_TO_CREATE ← "모임 생성하기" 버튼 표시

4. 방 생성
   User clicks "모임 생성하기"
   ↓
   CreateIntent.CreateRoom(userId, userName)
   ↓
   roomRepository.createRoom(...)
   ↓
   Firebase: rooms/{roomId} 생성
   Firebase: participants/{roomId}/{userId} 생성
   ↓
   CreateSideEffect.NavigateToMap(roomId, userId)
   ↓
   MapScreen
```

## UI State

### CreateUiState

```kotlin
data class CreateUiState(
    val selectedPlace: Place? = null,              // 선택된 장소
    val meetingPlaceName: String = "",             // 모임 장소 이름
    val selectedDate: Long? = null,                // 선택된 날짜 (밀리초)
    val selectedHour: Int? = null,                 // 선택된 시간
    val selectedMinute: Int? = null,               // 선택된 분
    val roomName: String = "",                     // 방 이름
    val currentStep: CreateStep = CreateStep.PLACE_SELECTION,  // 현재 단계
    val isCreating: Boolean = false,               // 생성 중 여부
    val error: String? = null                      // 에러 메시지
)
```

## Dependencies

### Domain Layer
```kotlin
- GetSelectedPlaceUseCase      // 선택된 장소 가져오기
- ClearSelectedPlaceUseCase    // 장소 선택 초기화
- RoomRepository              // 방 생성
```

### Core Modules
```kotlin
- core:designsystem           // AdegoDatePicker, AdegoTimePicker
- core:model                  // Place
- core:ui                     // clickableOnce extension
```

## 주요 클래스

### CreateViewModel

**파일 위치**: `feature/create/src/main/kotlin/com/teammanduk/adego/feature/create/CreateViewModel.kt`

**주요 메서드**:
- `onIntent(intent: CreateIntent)`: Intent 처리
- `reduce(intent: CreateIntent, state: CreateUiState)`: 상태 업데이트
- `calculateNextStep(state: CreateUiState)`: 다음 단계 계산
- `generateRoomName(placeName, hour, minute)`: 자동 방 이름 생성
- `createRoom(userId, userName)`: Firebase에 방 생성

## MVI Pattern

### Intent
```kotlin
sealed interface CreateIntent {
    data class SelectPlace(val place: Place) : CreateIntent
    data class UpdateMeetingPlaceName(val placeName: String) : CreateIntent
    data class SelectDate(val dateMillis: Long) : CreateIntent
    data class SelectTime(val hour: Int, val minute: Int) : CreateIntent
    data class UpdateRoomName(val roomName: String) : CreateIntent
    data class CreateRoom(val userId: String, val userName: String) : CreateIntent
    data object NavigateToSelectPlace : CreateIntent
    data object NavigateBack : CreateIntent
    data object ClearError : CreateIntent
}
```

### SideEffect
```kotlin
sealed interface CreateSideEffect {
    data object NavigateToSelectPlace : CreateSideEffect
    data object NavigateBack : CreateSideEffect
    data class NavigateToMap(val roomId: String, val userId: String) : CreateSideEffect
}
```

## 사용 예시

### 전체 플로우 시나리오

```kotlin
// 1. 사용자가 Home에서 "+ 새 모임 만들기" 클릭
// 2. CreateScreen 진입

// 3. "장소 선택" 필드 클릭
onIntent(CreateIntent.NavigateToSelectPlace)
// → SelectPlaceScreen으로 이동

// 4. SelectPlaceScreen에서 "강남역" 선택
// → GetSelectedPlaceUseCase()가 Place 방출
// → CreateViewModel이 자동으로 감지

onIntent(CreateIntent.SelectPlace(Place(
    name = "강남역",
    address = "서울 강남구 강남대로...",
    latitude = 37.4979,
    longitude = 127.0276
)))

// UI 자동 업데이트:
// - selectedPlace = Place(...)
// - meetingPlaceName = "강남역"
// - currentStep = TIME_SELECTION
// → "시간 선택" 섹션 표시

// 5. 날짜 선택: "2025년 10월 25일"
onIntent(CreateIntent.SelectDate(1729785600000))

// 6. 시간 선택: "14시 30분"
onIntent(CreateIntent.SelectTime(14, 30))

// UI 자동 업데이트:
// - roomName = "강남역 14시 30분 모임"
// - currentStep = ROOM_NAME_INPUT
// → "방 이름" 섹션 표시

// 7. (Optional) 사용자가 방 이름 수정
onIntent(CreateIntent.UpdateRoomName("강남역에서 만나요"))

// UI 자동 업데이트:
// - currentStep = READY_TO_CREATE
// → "모임 생성하기" 버튼 표시

// 8. "모임 생성하기" 클릭
onIntent(CreateIntent.CreateRoom(
    userId = "user_1729785600",
    userName = "호스트"
))

// CreateViewModel.createRoom() 실행:
// - Firebase에 방 생성
// - roomId = "ABC123" (자동 생성)
// → NavigateToMap("ABC123", "user_1729785600")
```

## 검증 규칙

| 필드 | 검증 |
|------|------|
| meetingPlaceName | 최대 30자 |
| roomName | 최대 20자 |
| selectedDate | null이 아님 |
| selectedHour | 0-23 |
| selectedMinute | 0-59 |

## 에러 처리

```kotlin
// 네트워크 에러
catch (e: Exception) {
    _uiState.update {
        it.copy(error = e.message ?: "알 수 없는 오류가 발생했습니다")
    }
}

// UI에서 AlertDialog 표시
error?.let { errorMessage ->
    AlertDialog(
        title = { Text("방 생성 실패") },
        text = { Text(errorMessage) }
    )
}
```

## 테스트 시나리오

1. **정상 플로우**
   - 모든 단계 완료 후 방 생성 성공
   - Map 화면으로 자동 이동

2. **단계별 UI 표시**
   - 장소 선택 전: TIME_SELECTION 섹션 숨김
   - 시간 선택 전: ROOM_NAME_INPUT 섹션 숨김
   - 모든 정보 입력 전: "모임 생성하기" 버튼 숨김

3. **입력 제한**
   - 장소 이름 30자 초과 입력 시 차단
   - 방 이름 20자 초과 입력 시 차단

4. **뒤로가기**
   - "뒤로가기" 버튼 클릭 시 Home으로 이동
   - 선택된 장소 정보 초기화

---

**관련 문서**
- [Feature: Place - 장소 선택](../place/README.md)
- [Feature: Map - 지도 화면](../map/README.md)
- [Core: Domain - Room Management](../../core/domain/README.md)
