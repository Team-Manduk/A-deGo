# Core: Navigation

타입 안전 네비게이션 정의를 제공하는 모듈입니다.

## 개요

Navigation 모듈은 Jetpack Compose Navigation의 타입 안전성을 보장하기 위한 Route 정의를 제공합니다. Kotlin Serialization을 사용하여 네비게이션 인자를 자동으로 직렬화/역직렬화합니다.

## Route 정의

### Route Sealed Interface

```kotlin
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Create : Route

    @Serializable
    data object SelectPlace : Route

    @Serializable
    data object SearchPlace : Route

    @Serializable
    data class Map(
        val roomId: String,
        val userId: String,
        val userName: String? = null
    ) : Route

    @Serializable
    data class SelectStartPlace(
        val roomId: String,
        val userId: String,
        val destLat: Double,
        val destLng: Double
    ) : Route

    @Serializable
    data class SelectRoute(
        val roomId: String,
        val userId: String,
        val startLat: Double,
        val startLng: Double,
        val destLat: Double,
        val destLng: Double
    ) : Route
}
```

## 네비게이션 플로우

```
Home
├─→ Create
│   └─→ SelectPlace
│       └─→ Map
└─→ Map
    └─→ SelectStartPlace
        └─→ SelectRoute
            └─→ Map (popBackStack)
```

## 사용 방법

### 1. NavHost 설정

```kotlin
@Composable
fun MainNavHost(navigator: MainNavigator) {
    NavHost(
        navController = navigator.navController,
        startDestination = Route.Home
    ) {
        composable<Route.Home> {
            HomeRoute(...)
        }

        composable<Route.Map> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Map>()
            MapRoute(
                roomId = args.roomId,
                userId = args.userId,
                userName = args.userName
            )
        }

        composable<Route.SelectRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.SelectRoute>()
            SelectRouteRoute(
                roomId = args.roomId,
                userId = args.userId,
                startLat = args.startLat,
                startLng = args.startLng,
                destLat = args.destLat,
                destLng = args.destLng
            )
        }
    }
}
```

### 2. 네비게이션 실행

```kotlin
// Simple route (no arguments)
navController.navigate(Route.Home)

// Route with arguments
navController.navigate(
    Route.Map(
        roomId = "ABC123",
        userId = "user_123",
        userName = "홍길동"
    )
)

// Complex route
navController.navigate(
    Route.SelectRoute(
        roomId = "ABC123",
        userId = "user_123",
        startLat = 37.4979,
        startLng = 127.0276,
        destLat = 37.5145,
        destLng = 127.1027
    )
)
```

### 3. 인자 읽기

```kotlin
composable<Route.Map> { backStackEntry ->
    val args = backStackEntry.toRoute<Route.Map>()

    MapRoute(
        roomId = args.roomId,      // "ABC123"
        userId = args.userId,      // "user_123"
        userName = args.userName   // "홍길동" or null
    )
}
```

## Route 유형

### Simple Routes (인자 없음)

```kotlin
data object Home : Route
data object Create : Route
data object SelectPlace : Route
data object SearchPlace : Route
```

단순히 화면 전환만 필요한 경우 사용합니다.

### Parameterized Routes (인자 있음)

```kotlin
data class Map(
    val roomId: String,
    val userId: String,
    val userName: String? = null
) : Route
```

화면 전환 시 데이터를 전달해야 하는 경우 사용합니다.

## 타입 안전성

### Before (String-based)

```kotlin
// ❌ 오타 가능, 타입 안전하지 않음
navController.navigate("map/$roomId/$userId")

// Arguments 파싱
val roomId = backStackEntry.arguments?.getString("roomId")  // String?
val userId = backStackEntry.arguments?.getString("userId")  // String?
```

### After (Type-safe)

```kotlin
// ✅ 컴파일 타임에 타입 체크
navController.navigate(Route.Map(roomId, userId))

// Arguments 자동 역직렬화
val args = backStackEntry.toRoute<Route.Map>()  // Map
val roomId = args.roomId  // String (non-null)
```

## Serialization

### Kotlin Serialization

Route는 `@Serializable` 어노테이션을 사용하여 자동으로 직렬화됩니다:

```kotlin
@Serializable
data class Map(
    val roomId: String,
    val userId: String,
    val userName: String? = null
) : Route
```

네비게이션 시 객체가 자동으로 URL 인코딩되어 전달되고, 목적지에서 자동으로 디코딩됩니다.

### 지원 타입

- `String`
- `Int`, `Long`, `Float`, `Double`
- `Boolean`
- Nullable types (`String?`, `Int?`, etc.)
- Custom Serializable classes (필요 시)

## Navigator Pattern

MainNavigator를 통한 중앙집중식 네비게이션 관리:

```kotlin
class MainNavigator(
    val navController: NavHostController,
    val startDestination: Route = Route.Home
) {
    fun navigateToCreate() {
        navController.navigate(Route.Create)
    }

    fun navigateToMap(roomId: String, userId: String, userName: String? = null) {
        navController.navigate(Route.Map(roomId, userId, userName))
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateHome() {
        navController.navigate(Route.Home) {
            popUpTo(Route.Home) { inclusive = true }
        }
    }
}
```

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.kotlinx.serialization.json)  // @Serializable
    implementation(libs.androidx.navigation.compose) // Navigation
}
```

## 장점

1. **타입 안전성**: 컴파일 타임에 인자 타입 검증
2. **오타 방지**: Route는 sealed interface로 정의되어 오타 불가능
3. **자동 완성**: IDE가 가능한 Route 목록을 자동으로 제안
4. **리팩토링 용이**: 인자명 변경 시 컴파일러가 모든 사용처를 찾아줌
5. **간결성**: 보일러플레이트 코드 감소

## 테스트

```kotlin
@Test
fun `Map Route는 필수 인자를 포함한다`() {
    val route = Route.Map(
        roomId = "ABC123",
        userId = "user_123",
        userName = null
    )

    assertEquals("ABC123", route.roomId)
    assertEquals("user_123", route.userId)
    assertNull(route.userName)
}
```

## 관련 문서

- [Feature: Main - Navigation](../../feature/main/README.md)
- [Jetpack Compose Navigation Documentation](https://developer.android.com/jetpack/compose/navigation)

---

**타입 안전 네비게이션의 핵심**:
- Sealed Interface로 모든 Route 정의
- Kotlin Serialization으로 자동 직렬화
- 컴파일 타임 타입 체크
