# Feature: Home

앱의 홈 화면으로, 방 생성 및 참여의 진입점 역할을 하는 Feature 모듈입니다.

## 개요

Home 모듈은 A-deGo 앱의 첫 화면으로, 사용자가 새로운 모임을 만들거나 초대 코드를 통해 기존 모임에 참여할 수 있는 인터페이스를 제공합니다.

## 주요 기능

### 1. 새 모임 만들기

- **큰 버튼 UI**: 눈에 잘 띄는 메인 액션
- **네비게이션**: Create 화면으로 이동

### 2. 초대 코드로 참여

- **코드 입력**: 영어 대문자와 숫자만 허용 (자동 변환)
- **방 검증**: Firebase에서 방 존재 여부 확인
- **자동 참여**: 임시 userId/userName 생성 후 자동 참여
- **Map 화면 이동**: 참여 성공 시 자동으로 Map 화면 전환

### 3. 알림 권한 요청

- **PermissionRequester**: 앱 최초 진입 시 알림 권한 요청
- **백그라운드 추적 안내**: 위치 정보 공유에 대한 사용자 안내

## 구조

### Components

#### HomeRoute (Composable)
- **역할**: 비즈니스 로직과 UI 연결
- **주요 기능**:
  - ViewModel 상태 관찰
  - 방 참여 성공 시 네비게이션
  - 에러 다이얼로그 표시
  - 알림 권한 요청

#### HomeScreen (Composable)
- **역할**: UI 렌더링
- **주요 구성 요소**:
  - 앱 타이틀 및 설명
  - "새 모임 만들기" 버튼
  - 초대 코드 입력 필드
  - "입장" 버튼
  - 위치 정보 안내 문구

#### HomeViewModel
- **역할**: 비즈니스 로직 및 상태 관리
- **주요 기능**:
  - 방 코드 검증
  - 방 참여 프로세스 관리
  - 에러 처리

## 데이터 플로우

### 방 참여 플로우

```
사용자가 초대 코드 입력 (예: "ABC123")
    ↓
"입장" 버튼 클릭
    ↓
HomeViewModel.joinRoomWithCode("ABC123")
    ↓
┌─────────────────────────────────────┐
│ 1. 방 존재 여부 확인                 │
│    roomRepository.getRoomInfo()     │
│    → Firebase: rooms/ABC123         │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 방이 존재하는가?                     │
├─────────────────────────────────────┤
│ NO  → "존재하지 않는 초대 코드"      │
│       에러 다이얼로그 표시            │
│                                     │
│ YES → 다음 단계                      │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 2. 임시 사용자 정보 생성             │
│    userId: "user_1234567890"        │
│    userName: "사용자"                │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 3. 방 참여                           │
│    roomRepository.joinRoom()        │
│    → Firebase: participants/ABC123  │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 참여 성공?                           │
├─────────────────────────────────────┤
│ NO  → "방 참여에 실패했습니다"       │
│       에러 다이얼로그 표시            │
│                                     │
│ YES → joinedRoomInfo 업데이트        │
└─────────────────────────────────────┘
    ↓
HomeRoute가 joinedRoomInfo 감지
    ↓
Map 화면으로 네비게이션
    (roomId: "ABC123", userId: "user_1234567890")
```

## UI 상태 관리

### ViewModel 상태

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {
    val isJoining: StateFlow<Boolean>        // 참여 진행 중 여부
    val error: StateFlow<String?>            // 에러 메시지
    val joinedRoomInfo: StateFlow<RoomJoinInfo?>  // 참여 성공 정보
}
```

### 상태별 UI 동작

| 상태 | UI 동작 |
|------|---------|
| `isJoining = true` | "입장" 버튼에 로딩 스피너 표시, 버튼 비활성화 |
| `error != null` | 에러 다이얼로그 표시 |
| `joinedRoomInfo != null` | Map 화면으로 자동 네비게이션 |

## 코드 입력 검증

### 입력 필터링

```kotlin
onValueChange = { newValue ->
    inviteCode = newValue
        .uppercase()                  // 소문자 → 대문자 변환
        .filter { it.isLetterOrDigit() }  // 영문자와 숫자만 허용
}
```

### 예시

| 사용자 입력 | 실제 저장 값 |
|------------|-------------|
| `abc123` | `ABC123` |
| `AB-C12 3` | `ABC123` |
| `테스트123` | `123` |

## Dependencies

### Domain Layer
```kotlin
implementation(project(":core:domain"))
- RoomRepository (방 정보 조회 및 참여)
```

### Core Modules
```kotlin
implementation(project(":core:designsystem"))
- AdegoTheme (테마, 타이포그래피, 색상)

implementation(project(":core:ui"))
- PermissionRequester (알림 권한 요청)
- PermissionType.Notification
```

### Android Libraries
```kotlin
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.hilt.navigation.compose)
implementation(libs.androidx.lifecycle.runtime.compose)
```

## 주요 클래스

### HomeViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel()
```

**파일 위치**: `feature/home/src/main/kotlin/com/teammanduk/adego/feature/home/HomeViewModel.kt`

**주요 메서드**:
- `joinRoomWithCode(inviteCode: String)`: 초대 코드로 방 참여
- `clearError()`: 에러 메시지 초기화
- `clearJoinedRoomInfo()`: 참여 정보 초기화

### HomeRoute

```kotlin
@Composable
internal fun HomeRoute(
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMap: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
)
```

**파일 위치**: `feature/home/src/main/kotlin/com/teammanduk/adego/feature/home/HomeScreen.kt:48`

### HomeScreen

```kotlin
@Composable
private fun HomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onJoinWithCode: (String) -> Unit,
    isJoining: Boolean
)
```

**파일 위치**: `feature/home/src/main/kotlin/com/teammanduk/adego/feature/home/HomeScreen.kt:94`

## UI 컴포넌트

### 레이아웃 구조

```
Box (전체 화면)
├── IconButton (설정, 우상단)
└── Column (중앙 정렬)
    ├── Text ("지금 만나기로 했나요?")
    ├── Text ("바로 시작해 보세요")
    ├── Button ("+ 새 모임 만들기")
    ├── Text ("초대 코드로 시작하기")
    ├── Row
    │   ├── OutlinedTextField (코드 입력)
    │   └── Button ("입장")
    └── Text (안내 문구)
```

### 디자인 토큰 사용

```kotlin
// 색상
AdegoTheme.colors.background      // 배경색
AdegoTheme.colors.main500         // 메인 컬러
AdegoTheme.colors.main900         // 진한 메인 컬러
AdegoTheme.colors.onMain500       // 메인 컬러 위 텍스트
AdegoTheme.colors.line500         // 보조 텍스트 및 테두리

// 타이포그래피
AdegoTheme.typography.headlineLarge  // 타이틀
AdegoTheme.typography.titleLarge     // 섹션 제목
AdegoTheme.typography.bodyLarge      // 본문
AdegoTheme.typography.bodySmall      // 안내 문구
```

## 사용 예시

### 네비게이션 연결

```kotlin
// MainNavHost에서 호출
homeNavGraph(
    onNavigateToCreate = navigator::navigateToCreate,
    onNavigateToSettings = { /* TODO: 설정 화면 */ },
    onNavigateToMap = navigator::navigateToMap
)
```

### 방 참여 성공 시나리오

```kotlin
// 1. 사용자가 "ABC123" 입력
// 2. "입장" 버튼 클릭
// 3. HomeViewModel.joinRoomWithCode("ABC123")

// ViewModel 내부:
val roomResult = roomRepository.getRoomInfo("ABC123")
// → Firebase에서 방 정보 확인
// → room.roomName = "강남역에서 만나요"

val tempUserId = "user_1705123456789"
val joinResult = roomRepository.joinRoom(
    roomId = "ABC123",
    userId = tempUserId,
    userName = "사용자"
)
// → Firebase participants/ABC123/user_1705123456789 생성

_joinedRoomInfo.value = RoomJoinInfo(
    roomId = "ABC123",
    userId = "user_1705123456789"
)

// HomeRoute에서:
LaunchedEffect(joinedRoomInfo) {
    joinedRoomInfo?.let { info ->
        onNavigateToMap(info.roomId, info.userId)
        // → Map 화면으로 이동
    }
}
```

### 에러 처리 시나리오

```kotlin
// 존재하지 않는 방 코드 입력 시:
val roomResult = roomRepository.getRoomInfo("INVALID")
// → room == null

_error.value = "존재하지 않는 초대 코드입니다."

// HomeRoute에서:
error?.let { errorMessage ->
    AlertDialog(
        title = { Text("방 참여 실패") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(onClick = { viewModel.clearError() }) {
                Text("확인")
            }
        }
    )
}
```

## 권한 요청

### 알림 권한 (Android 13+)

```kotlin
PermissionRequester(
    permissionTypes = listOf(PermissionType.Notification)
) {
    HomeScreen(...)  // 권한 승인 후 화면 표시
}
```

**권한 필요 이유**:
- 백그라운드 위치 추적 서비스는 Foreground Service로 동작
- Foreground Service는 알림을 필수로 표시해야 함
- Android 13 이상에서는 알림 권한 필요

## 로깅

HomeViewModel에는 상세한 로그가 추가되어 있습니다:

```kotlin
Log.d(TAG, "[HomeViewModel] 초대코드로 방 참여 시도: $inviteCode")
Log.d(TAG, "[HomeViewModel] 방 정보 조회 성공: ${room.roomName}")
Log.d(TAG, "[HomeViewModel] 방 참여 시도 - userId: $userId, userName: $userName")
Log.d(TAG, "[HomeViewModel] 방 참여 성공")
Log.e(TAG, "[HomeViewModel] 방 참여 실패", exception)
```

**로그 필터**: `A-degoLogTag`

## 개선 가능 사항

### 현재 구현

- 임시 userId: `"user_{timestamp}"`
- 임시 userName: `"사용자"`
- Map 화면에서 사용자 이름 변경 다이얼로그 표시

### 향후 개선

- [ ] Home 화면에서 바로 사용자 이름 입력받기
- [ ] QR 코드 스캔 기능 추가
- [ ] 최근 참여한 방 목록 표시
- [ ] 설정 화면 구현 (현재 TODO)
- [ ] 방 코드 형식 검증 (6자리 등)

## 테스트 시나리오

### 정상 플로우

1. **방 생성 네비게이션**
   - "새 모임 만들기" 버튼 클릭
   - Create 화면으로 이동 확인

2. **방 참여 성공**
   - 유효한 초대 코드 입력
   - 방 정보 조회 성공
   - 방 참여 성공
   - Map 화면으로 자동 이동

### 에러 처리

1. **잘못된 초대 코드**
   - 존재하지 않는 코드 입력
   - 에러 다이얼로그 표시
   - "존재하지 않는 초대 코드입니다." 메시지 확인

2. **네트워크 에러**
   - 오프라인 상태에서 방 참여 시도
   - 적절한 에러 메시지 표시

### UI 상태

1. **로딩 상태**
   - 방 참여 진행 중 버튼 비활성화
   - 로딩 스피너 표시

2. **입력 검증**
   - 코드 입력 전 버튼 비활성화
   - 영문자 소문자 → 대문자 자동 변환
   - 특수문자 자동 제거

## 스크린샷

(실제 프로젝트에 스크린샷 추가 권장)

---

**관련 문서**
- [Feature: Main - Navigation](../main/README.md)
- [Feature: Create - 방 생성](../create/README.md)
- [Feature: Map - 지도 화면](../map/README.md)
- [Core: UI - PermissionRequester](../../core/ui/README.md)
