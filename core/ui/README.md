# Core: UI

공통 UI 컴포넌트 및 유틸리티를 제공하는 모듈입니다.

## 개요

UI 모듈은 여러 Feature 모듈에서 공통으로 사용하는 Composable 컴포넌트, Modifier Extension, 권한 요청 시스템 등을 제공합니다.

## 구조

```
core/ui/
├── component/          # 공통 UI 컴포넌트
│   └── LoadingScreen
├── permission/         # 권한 요청 시스템
│   ├── PermissionRequester
│   ├── PermissionType
│   ├── PermissionState
│   └── PermissionDialog
├── extension/          # Modifier Extension
│   └── ModifierExtensions
└── util/               # UI 유틸리티
    └── FormatUtils
```

## 주요 컴포넌트

### 1. PermissionRequester

Android 런타임 권한 요청을 위한 선언적 Composable

**기능**:
- 자동 권한 상태 확인
- 권한 요청 다이얼로그 표시
- 영구 거부 시 설정 화면으로 안내
- 라이프사이클 인식 (앱 재진입 시 자동 재확인)

**사용 예시**:
```kotlin
@Composable
fun HomeRoute() {
    PermissionRequester(
        permissionTypes = listOf(PermissionType.Notification),
        onGranted = {
            // 권한 허용 시
        },
        onDenied = {
            // 권한 거부 시
        }
    ) {
        // 권한 허용 후 표시할 컨텐츠
        HomeScreen()
    }
}
```

**다중 권한 요청**:
```kotlin
PermissionRequester(
    permissionTypes = listOf(
        PermissionType.Location,
        PermissionType.Notification
    )
) {
    MapScreen()
}
```

### 2. PermissionType

권한 타입 정의 (Sealed Class)

**지원 권한**:

#### Location
```kotlin
PermissionType.Location
// 포함 권한:
// - ACCESS_FINE_LOCATION
// - ACCESS_COARSE_LOCATION

// 설명:
rationaleTitle = "위치 권한 필요"
rationaleText = "실시간 위치 추적 및 모임 장소까지의 거리를 확인하기 위해 위치 권한이 필요합니다."
```

#### Notification
```kotlin
PermissionType.Notification
// 포함 권한 (Android 13+):
// - POST_NOTIFICATIONS

// 설명:
rationaleTitle = "알림 권한 필요"
rationaleText = "위치 공유 중임을 알리고 백그라운드에서도 위치를 추적하기 위해 알림 권한이 필요합니다."
```

**커스텀 권한 추가**:
```kotlin
data object Camera : PermissionType() {
    override val permissions = arrayOf(Manifest.permission.CAMERA)
    override val rationaleTitle = "카메라 권한 필요"
    override val rationaleText = "QR 코드 스캔을 위해 카메라 권한이 필요합니다."
    override val settingsTitle = "카메라 권한 필요"
    override val settingsText = "설정에서 카메라 권한을 허용해주세요."
}
```

### 3. PermissionState

권한 상태를 나타내는 Sealed Interface

```kotlin
sealed interface PermissionState {
    data object Idle : PermissionState              // 초기 상태
    data object Granted : PermissionState            // 권한 허용
    data object DeniedTemporary : PermissionState    // 일시적 거부 (재요청 가능)
    data object DeniedPermanently : PermissionState  // 영구 거부 (설정 필요)
}
```

**상태별 UI 동작**:
| 상태 | UI |
|------|-----|
| `Granted` | 컨텐츠 표시 |
| `DeniedTemporary` | 권한 설명 다이얼로그 + "다시 요청" 버튼 |
| `DeniedPermanently` | 설정 이동 안내 다이얼로그 + "설정 열기" 버튼 |

### 4. LoadingScreen

전체 화면 로딩 인디케이터

```kotlin
@Composable
fun LoadingScreen(
    text: String = "로딩 중..."
)
```

**사용 예시**:
```kotlin
when (val state = sessionState) {
    SessionState.Loading -> LoadingScreen(text = "세션 확인 중...")
    is SessionState.Restored -> MainContent()
    SessionState.None -> LoginScreen()
}
```

## Modifier Extensions

### clickableOnce

중복 클릭을 방지하는 Modifier

**문제**:
```kotlin
// ❌ 빠르게 여러 번 클릭 시 중복 네비게이션 발생
Button(onClick = { navigateToNextScreen() }) {
    Text("다음")
}
```

**해결**:
```kotlin
// ✅ 첫 클릭 후 자동으로 비활성화
OutlinedTextField(
    modifier = Modifier.clickableOnce {
        navigateToNextScreen()
    }
)
```

**파라미터**:
```kotlin
fun Modifier.clickableOnce(
    enabled: Boolean = true,        // 클릭 가능 여부
    showRipple: Boolean = false,    // 리플 효과 표시 여부
    onClick: () -> Unit
): Modifier
```

**사용 예시**:
```kotlin
// CreateScreen에서 장소 선택 필드
OutlinedTextField(
    value = selectedPlace?.address ?: "",
    modifier = Modifier
        .fillMaxWidth()
        .clickableOnce { onNavigateToSelectPlace() },
    enabled = false  // TextField 비활성화, clickable로만 동작
)
```

## UI Utilities

### FormatUtils

UI 표시용 포맷팅 유틸리티

```kotlin
object FormatUtils {
    fun formatDistance(meters: Int): String
    fun formatTime(minutes: Int): String
    fun formatFare(won: Int): String
}
```

**사용 예시**:
```kotlin
// 거리 포맷팅
FormatUtils.formatDistance(1250)  // "1.3km"
FormatUtils.formatDistance(350)   // "350m"

// 시간 포맷팅
FormatUtils.formatTime(65)        // "1시간 5분"
FormatUtils.formatTime(45)        // "45분"

// 요금 포맷팅
FormatUtils.formatFare(1400)      // "1,400원"
```

## 권한 요청 플로우

### 정상 플로우

```
PermissionRequester 진입
    ↓
권한 상태 확인
    ↓
┌─────────────────────────────┐
│ 이미 허용됨?                 │
├─────────────────────────────┤
│ YES → content() 즉시 표시    │
│                             │
│ NO  → 권한 요청 다이얼로그   │
└─────────────────────────────┘
    ↓
사용자가 "허용" 클릭
    ↓
PermissionState.Granted
    ↓
content() 표시
```

### 일시적 거부 플로우

```
사용자가 "거부" 클릭 (첫 번째)
    ↓
PermissionState.DeniedTemporary
    ↓
PermissionRationaleDialog 표시
    ↓
"권한이 필요한 이유" 설명
    ↓
┌─────────────────────────────┐
│ 사용자 선택                  │
├─────────────────────────────┤
│ "다시 요청" → 권한 요청 재시작│
│ "취소" → onDenied() 콜백     │
└─────────────────────────────┘
```

### 영구 거부 플로우

```
사용자가 "거부" + "다시 묻지 않음" 체크
    ↓
PermissionState.DeniedPermanently
    ↓
GoToSettingsDialog 표시
    ↓
"설정에서 권한을 허용해주세요" 안내
    ↓
┌─────────────────────────────┐
│ 사용자 선택                  │
├─────────────────────────────┤
│ "설정 열기" → 앱 설정 화면   │
│ "취소" → onDenied() 콜백     │
└─────────────────────────────┘
    ↓
설정에서 권한 허용
    ↓
앱으로 복귀 (ON_RESUME)
    ↓
자동으로 권한 상태 재확인
    ↓
PermissionState.Granted
    ↓
content() 표시
```

## 권한 메시지 커스터마이징

### 다중 권한 메시지 조합

```kotlin
val permissionTypes = listOf(
    PermissionType.Location,
    PermissionType.Notification
)

val message = permissionTypes.toPermissionMessage()

// 결과:
message.title = "위치 및 알림 권한 필요"
message.text = """
    실시간 위치 추적 및 모임 장소까지의 거리를 확인하기 위해 위치 권한이 필요합니다.

    위치 공유 중임을 알리고 백그라운드에서도 위치를 추적하기 위해 알림 권한이 필요합니다.
"""
```

### 커스텀 구분자

```kotlin
permissionTypes.toPermissionMessage(
    titleSeparator = ", ",
    textSeparator = "\n\n"
)
```

## 라이프사이클 인식

PermissionRequester는 앱 라이프사이클을 자동으로 추적합니다:

```kotlin
DisposableEffect(lifecycleOwner, isAlreadyRequest) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME && isAlreadyRequest) {
            // 설정에서 돌아온 경우 권한 재확인
            state = permissions.toPermissionState(context, activity, requestedOnce = true)
        }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

**시나리오**:
1. 사용자가 권한 영구 거부
2. "설정 열기" 클릭 → 설정 앱으로 이동
3. 설정에서 권한 허용
4. 뒤로가기로 앱 복귀 (ON_RESUME)
5. PermissionRequester가 자동으로 권한 재확인
6. content() 자동 표시

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)  // rememberLauncherForActivityResult
    implementation(libs.androidx.lifecycle.runtime.compose)
}
```

## 사용 예시

### HomeScreen (알림 권한)

```kotlin
@Composable
internal fun HomeRoute(
    onNavigateToCreate: () -> Unit,
    onNavigateToMap: (String, String) -> Unit
) {
    PermissionRequester(
        permissionTypes = listOf(PermissionType.Notification)
    ) {
        HomeScreen(
            onNavigateToCreate = onNavigateToCreate,
            onJoinWithCode = { code -> /* ... */ }
        )
    }
}
```

### MapScreen (위치 권한)

```kotlin
@Composable
internal fun MapRoute(
    roomId: String,
    userId: String
) {
    PermissionRequester(
        permissionTypes = listOf(PermissionType.Location),
        onGranted = {
            // 위치 추적 시작
            startLocationTracking()
        },
        onDenied = {
            // 위치 없이 제한된 기능만 제공
            showLimitedFeaturesMessage()
        }
    ) {
        MapScreen(roomId, userId)
    }
}
```

## 테스트

```kotlin
@Test
fun `clickableOnce는 첫 클릭 후 비활성화된다`() = runTest {
    var clickCount = 0

    composeTestRule.setContent {
        Box(
            modifier = Modifier
                .clickableOnce { clickCount++ }
                .testTag("box")
        ) {
            Text("Click me")
        }
    }

    composeTestRule.onNodeWithTag("box").performClick()
    composeTestRule.onNodeWithTag("box").performClick()
    composeTestRule.onNodeWithTag("box").performClick()

    assertEquals(1, clickCount)  // 한 번만 클릭됨
}
```

## 관련 문서

- [Feature: Home - 권한 요청](../../feature/home/README.md)
- [Feature: Map - 위치 권한](../../feature/map/README.md)
- [Android Permissions Documentation](https://developer.android.com/training/permissions/requesting)

---

**Core UI의 핵심 원칙**:
1. 재사용 가능한 공통 컴포넌트
2. 선언적 권한 요청 시스템
3. 사용자 경험 개선 (중복 클릭 방지 등)
