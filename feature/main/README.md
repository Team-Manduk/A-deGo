# Feature: Main

메인 네비게이션 및 세션 관리를 담당하는 Feature 모듈입니다.

## 개요

Main 모듈은 A-deGo 앱의 진입점이자 중앙 네비게이션 허브 역할을 합니다. 사용자 세션을 관리하고, 앱 시작 시 적절한 화면으로 라우팅하며, 딥링크 처리를 담당합니다.

## 주요 기능

### 1. 세션 관리

앱 시작 시 저장된 세션을 확인하고 자동으로 복원합니다:

- **세션 복원**: 이전 세션 정보(roomId, userId, userName)가 있으면 Map 화면으로 직접 이동
- **신규 사용자**: 세션 정보가 없으면 Home 화면부터 시작
- **로딩 상태**: 세션 확인 중 로딩 화면 표시

### 2. 네비게이션 관리

앱 내 모든 Feature 모듈 간 네비게이션을 조율합니다:

```
Home → Create → SelectPlace → Map → SelectStartPlace → SelectRoute
  ↓                                      ↑
  └──────────────────────────────────────┘
```

### 3. 딥링크 처리

다양한 딥링크 형식을 지원합니다:

- `adego://join/{roomId}` - 커스텀 스킴
- `https://a-dego.web.app/join/{roomId}` - 웹 링크
- `https://adego.kr/join/{roomId}` - 커스텀 도메인

## 구조

### Components

#### MainActivity
- **역할**: 앱의 진입점, 세션 상태에 따른 초기 라우팅
- **주요 기능**:
  - 세션 상태 관찰 및 초기 화면 결정
  - 딥링크 처리 및 파싱
  - EdgeToEdge UI 설정

#### MainViewModel
- **역할**: 세션 상태 관리
- **주요 기능**:
  - `RestoreSessionUseCase`를 통한 세션 복원
  - `SessionState` 관리 (Loading, Restored, None)

#### MainNavHost
- **역할**: 네비게이션 그래프 구성
- **주요 기능**:
  - 모든 Feature 모듈의 NavGraph 통합
  - MainNavigator를 통한 화면 전환 관리

#### MainNavigator
- **역할**: 타입 안전 네비게이션 API 제공
- **주요 기능**:
  - 각 화면으로의 네비게이션 메서드 제공
  - 백 스택 관리

## 세션 상태 플로우

```kotlin
sealed interface SessionState {
    data object Loading : SessionState
    data object None : SessionState
    data class Restored(val sessionInfo: SessionInfo) : SessionState
}
```

### 세션 복원 플로우

```
App Start
    ↓
MainActivity.onCreate()
    ↓
MainViewModel.checkSession()
    ↓
RestoreSessionUseCase.invoke()
    ↓
SessionDataSource.getSessionInfo()
    ↓
┌─────────────────────────────┐
│ Session 있음?                │
├─────────────────────────────┤
│ YES → SessionState.Restored │
│       → Map 화면으로 시작     │
│                             │
│ NO  → SessionState.None     │
│       → Home 화면으로 시작    │
└─────────────────────────────┘
```

## 딥링크 처리

### 지원 형식

| 형식 | 예시 | 설명 |
|------|------|------|
| Custom Scheme | `adego://join/ABC123` | 앱 간 공유용 |
| HTTPS (Firebase) | `https://a-dego.web.app/join/ABC123` | Dynamic Links |
| HTTPS (Custom) | `https://adego.kr/join/ABC123` | 커스텀 도메인 |

### 처리 로직

```kotlin
private fun handleDeepLink(intent: Intent, navigator: MainNavigator) {
    val data = intent.data ?: return

    val roomId = when {
        // adego://join/{roomId}
        data.scheme == "adego" && data.host == "join" -> {
            data.pathSegments.firstOrNull()
        }
        // https://.../join/{roomId}
        (data.scheme == "http" || data.scheme == "https") &&
        data.pathSegments.firstOrNull() == "join" -> {
            data.pathSegments.getOrNull(1)
        }
        else -> null
    }

    if (!roomId.isNullOrBlank()) {
        val userId = UUID.randomUUID().toString()
        navigator.navigateToMap(roomId, userId)
    }
}
```

## 네비게이션 그래프

### 등록된 NavGraph

```kotlin
NavHost {
    homeNavGraph(...)          // feature:home
    createNavGraph(...)        // feature:create
    placeNavGraph(...)         // feature:place
    mapNavGraph(...)           // feature:map
    selectRouteNavGraph(...)   // feature:route
}
```

### 주요 네비게이션 경로

| From | Action | To | Navigator Method |
|------|--------|-----|------------------|
| Home | 방 생성 | Create | `navigateToCreate()` |
| Home | 방 참여 | Map | `navigateToMap(roomId, userId)` |
| Create | 장소 선택 | SelectPlace | `navigateToSelectPlace(roomId, userId)` |
| SelectPlace | 방 생성 완료 | Map | `navigateToMap(roomId, userId)` |
| Map | 경로 선택 | SelectStartPlace | `navigateToSelectStartPlace(...)` |
| SelectStartPlace | 출발지 선택 | SelectRoute | `navigateToSelectRoute(...)` |
| SelectRoute | 경로 선택 완료 | Map (pop) | `popBackStack()` |
| Map | 방 나가기 | Home | `navigateHome()` |

## Dependencies

### Domain Layer
```kotlin
implementation(project(":core:domain"))
- RestoreSessionUseCase
- SessionInfo
```

### Feature Modules
```kotlin
implementation(project(":feature:home"))
implementation(project(":feature:create"))
implementation(project(":feature:map"))
implementation(project(":feature:place"))
implementation(project(":feature:route"))
```

### Core Modules
```kotlin
implementation(project(":core:navigation"))
implementation(project(":core:designsystem"))
implementation(project(":core:ui"))
```

## 주요 클래스

### MainActivity
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 세션 상태에 따른 초기 화면 결정
        // 딥링크 처리
    }
}
```

**파일 위치**: `feature/main/src/main/kotlin/com/teammanduk/adego/feature/main/MainActivity.kt`

### MainViewModel
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSession: RestoreSessionUseCase
) : ViewModel() {
    val sessionState: StateFlow<SessionState>
}
```

**파일 위치**: `feature/main/src/main/kotlin/com/teammanduk/adego/feature/main/MainViewModel.kt`

### MainNavigator
```kotlin
class MainNavigator(
    val navController: NavHostController,
    val startDestination: Route
) {
    fun navigateToCreate()
    fun navigateToMap(roomId: String, userId: String, userName: String? = null)
    fun navigateToSelectPlace(roomId: String, userId: String)
    // ... more navigation methods
}
```

**파일 위치**: `feature/main/src/main/kotlin/com/teammanduk/adego/feature/main/navigation/MainNavigator.kt`

## 사용 예시

### 세션 복원 시나리오

```kotlin
// 1. 사용자가 앱 종료 전 Map 화면에 있었음
// 2. 앱 재실행
// 3. MainActivity 시작

// MainActivity.kt
setContent {
    val sessionState by viewModel.sessionState.collectAsState()

    when (val state = sessionState) {
        SessionState.Loading -> LoadingScreen()

        is SessionState.Restored -> {
            // 이전 세션 정보로 Map 화면부터 시작
            val navigator = rememberMainNavigator(
                startDestination = Route.Map(
                    roomId = state.sessionInfo.roomId,
                    userId = state.sessionInfo.userId,
                    userName = state.sessionInfo.userName
                )
            )
            MainRoute(navigator)
        }

        SessionState.None -> {
            // Home 화면부터 시작
            val navigator = rememberMainNavigator()
            MainRoute(navigator)
        }
    }
}
```

### 딥링크 처리 시나리오

```kotlin
// 사용자가 "https://a-dego.web.app/join/ABC123" 링크 클릭

// MainActivity.onCreate() or onNewIntent()
handleDeepLink(intent, navigator)

// 파싱 결과: roomId = "ABC123"
// 새 userId 생성: "550e8400-e29b-41d4-a716-446655440000"
// Map 화면으로 네비게이션
navigator.navigateToMap(
    roomId = "ABC123",
    userId = "550e8400-e29b-41d4-a716-446655440000"
)
```

## 설정

### AndroidManifest.xml

딥링크를 처리하기 위한 Intent Filter 설정:

```xml
<activity android:name=".MainActivity">
    <!-- Custom Scheme -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="adego" android:host="join" />
    </intent-filter>

    <!-- HTTPS Links -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="a-dego.web.app" />
        <data android:scheme="https" android:host="adego.kr" />
    </intent-filter>
</activity>
```

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────┐
│              MainActivity                        │
│  - 세션 상태 관찰                                 │
│  - 딥링크 처리                                    │
│  - 초기 라우팅                                    │
└────────────────┬────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────────┐
│            MainViewModel                         │
│  - RestoreSessionUseCase 호출                    │
│  - SessionState 관리                             │
└────────────────┬────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────────┐
│            MainNavHost                           │
│  - 모든 Feature NavGraph 통합                    │
│  - Navigation 조율                               │
└─────────────────────────────────────────────────┘
                 │
        ┌────────┼────────┐
        ↓        ↓        ↓
     Home    Create    Map  ...
```

## 테스트

### 테스트할 시나리오

1. **세션 복원**
   - 세션 정보가 있을 때 Map 화면으로 시작되는지 확인
   - 세션 정보가 없을 때 Home 화면으로 시작되는지 확인

2. **딥링크 처리**
   - Custom Scheme 링크가 올바르게 파싱되는지 확인
   - HTTPS 링크가 올바르게 파싱되는지 확인
   - 잘못된 형식의 링크를 무시하는지 확인

3. **네비게이션**
   - 각 화면 간 네비게이션이 정상 작동하는지 확인
   - 백 스택이 올바르게 관리되는지 확인

## 향후 개선 사항

- [ ] 설정 화면 구현
- [ ] Dynamic Links 고급 기능 (attribution, analytics)
- [ ] 세션 만료 처리
- [ ] 오프라인 모드 지원

---

**관련 문서**
- [Core: Domain - Session Management](../../core/domain/README.md)
- [Core: Navigation](../../core/navigation/README.md)
- [Feature: Home](../home/README.md)
- [Feature: Map](../map/README.md)
