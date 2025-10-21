# sync:location

백그라운드 위치 추적을 담당하는 모듈

## 구조

```
sync/location/
├── manager/
│   ├── LocationTrackingManager.kt       # 위치 추적 제어 인터페이스
│   └── LocationTrackingManagerImpl.kt   # 구현체
├── service/
│   └── LocationTrackingService.kt       # Foreground Service
└── di/
    └── LocationSyncModule.kt            # Hilt DI 모듈
```

## 주요 컴포넌트

### LocationTrackingManager (인터페이스)

```kotlin
interface LocationTrackingManager {
    fun startTracking()
    fun stopTracking()
    fun isTracking(): StateFlow<Boolean>
}
```

**역할:**
- ViewModel에서 Service 제어
- Context 의존성 캡슐화
- 테스트 가능성 제공

### LocationTrackingManagerImpl (구현체)

```kotlin
@Singleton
class LocationTrackingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationTrackingManager
```

**동작:**
- `startTracking()`: `startForegroundService()` 호출
- `stopTracking()`: `stopService()` 호출
- Android 8.0 이상/이하 분기 처리

### LocationTrackingService (Foreground Service)

```kotlin
@AndroidEntryPoint
class LocationTrackingService : Service()
```

**생명주기:**
```
onStartCommand()
  → Foreground 시작 (알림 표시)
  → BroadcastLocationUseCase 구독

onDestroy()
  → serviceScope 취소
  → Foreground 중지
```

**주입 의존성:**
- `@Named("LocationTracking") notifier: Notifier` - 알림 생성
- `broadcastLocation: BroadcastLocationUseCase` - 위치 브로드캐스트
- `userRepository`, `roomRepository` - (현재 미사용, 제거 예정)

**독립적 생명주기:**
```kotlin
private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

serviceScope.launch {
    broadcastLocation()
        .catch { e -> Log.e(TAG, "Location tracking error", e) }
        .collect { location -> Log.d(TAG, "Location updated: $location") }
}
```

- ViewModel과 독립적으로 동작
- 앱이 백그라운드로 가도 위치 추적 지속
- Service 종료 시 자동 정리

## 동작 흐름

### 1. 위치 추적 시작

```
MapViewModel.startLocationTracking()
  ↓
LocationTrackingManager.startTracking()
  ↓
context.startForegroundService(LocationTrackingService)
  ↓
LocationTrackingService.onStartCommand()
  ├─ notifier.createNotification() → startForeground()
  └─ broadcastLocation() 구독 시작
       ↓
     BroadcastLocationUseCase
       ↓
     LocationRepository.observeLocationUpdates()
       ↓
     FusedLocationDataSource (5초마다 위치 업데이트)
       ↓
     UpdateLocationUseCase (Firebase 업로드)
```

### 2. 백그라운드 동작

```
앱 최소화 / 화면 꺼짐
  ↓
MapViewModel.onCleared() (ViewModel 파괴)
  ↓
LocationTrackingService는 계속 실행 (독립적 생명주기)
  ↓
serviceScope에서 broadcastLocation() Flow 계속 구독
  ↓
위치 업데이트 계속 수집 및 Firebase 업로드
```

### 3. 위치 추적 종료

```
MapViewModel.leaveRoomAndNavigateHome()
  ↓
LocationTrackingManager.stopTracking()
  ↓
context.stopService(LocationTrackingService)
  ↓
LocationTrackingService.onDestroy()
  ├─ serviceScope.cancel() (Flow 구독 중지)
  └─ stopForeground(STOP_FOREGROUND_REMOVE) (알림 제거)
```

## AndroidManifest 설정

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".service.LocationTrackingService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

## DI 구성

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationSyncModule {

    @Binds
    abstract fun bindsLocationTrackingManager(
        manager: LocationTrackingManagerImpl
    ): LocationTrackingManager
}
```

## 사용 방법 (ViewModel)

```kotlin
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationTrackingManager: LocationTrackingManager
) : ViewModel() {

    private fun startLocationTracking() {
        locationTrackingManager.startTracking()
        _uiState.update { it.copy(isLocationTrackingActive = true) }
    }

    private fun leaveRoomAndNavigateHome() {
        viewModelScope.launch {
            locationTrackingManager.stopTracking()
            // ...
        }
    }
}
```

## 핵심 설계 원칙

### Clean Architecture
- Service → UseCase → Repository 계층 준수
- Domain 레이어만 의존 (UI 레이어 의존 없음)

### 단일 책임
- LocationTrackingManager: Service 제어만
- LocationTrackingService: Foreground Service 실행 + Flow 구독만
- BroadcastLocationUseCase: 위치 수집 + 업로드 로직

### 생명주기 분리
- ViewModel: UI 생명주기
- Service: 독립적 생명주기 (백그라운드 동작)

### 데이터 흐름
- Repository가 session 데이터 관리 (단일 진실 공급원)
- Intent로 데이터 전달하지 않음
- UseCase가 Repository에서 필요한 데이터 조회

## 의존성

```kotlin
dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.notifications)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
```

## 주의사항

1. **중복 구독 방지**
   - Service와 ViewModel에서 BroadcastLocationUseCase를 동시에 구독하지 않음
   - 현재는 Service에서만 구독

2. **Foreground Service 제약**
   - Android 8.0+ 에서는 `startForeground()`를 5초 내에 호출해야 함
   - 현재는 `onStartCommand()`에서 즉시 호출

3. **배터리 최적화**
   - 위치 업데이트 간격: 5초 (LOCATION_UPDATE_INTERVAL)
   - 필요시 간격 조정 가능

4. **백그라운드 위치 제한**
   - Foreground Service로 실행하므로 백그라운드 제한 회피
   - `foregroundServiceType="location"` 필수
