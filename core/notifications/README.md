# core:notifications

알림 생성을 담당하는 모듈

## 구조

```
core/notifications/
├── Notifier.kt                    # 알림 생성 인터페이스
├── LocationTrackingNotifier.kt    # 위치 추적 알림 구현체
└── di/
    └── NotificationsModule.kt     # Hilt DI 모듈
```

## 주요 컴포넌트

### Notifier (인터페이스)

```kotlin
interface Notifier {
    fun createNotification(): Notification
}
```

- 역할: 알림 생성 추상화
- 목적: 테스트 가능성, 구현체 교체 가능성

### LocationTrackingNotifier (구현체)

```kotlin
@Singleton
class LocationTrackingNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Notifier
```

**책임:**
- 위치 추적 Foreground Service 알림 생성
- Notification Channel 관리
- PendingIntent 생성

**알림 특성:**
- Channel ID: `location_tracking_channel`
- Importance: `IMPORTANCE_LOW` (소리/진동 없음)
- Ongoing: `true` (스와이프로 제거 불가)
- Foreground Service Behavior: `FOREGROUND_SERVICE_IMMEDIATE`

**Notification ID:**
```kotlin
companion object {
    const val NOTIFICATION_ID = 1001
}
```

## DI 구성

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {

    @Binds
    @Named("LocationTracking")
    abstract fun bindsLocationTrackingNotifier(
        notifier: LocationTrackingNotifier
    ): Notifier
}
```

- `@Named("LocationTracking")`: 여러 Notifier 구현체 구분
- `@Singleton`: 앱 전체에서 단일 인스턴스

## 사용 방법

```kotlin
@Inject
@Named("LocationTracking")
lateinit var notifier: Notifier

val notification = notifier.createNotification()
startForeground(LocationTrackingNotifier.NOTIFICATION_ID, notification)
```

## 확장 방법

새로운 알림 타입 추가:

1. `Notifier` 인터페이스 구현
```kotlin
@Singleton
class ArrivalNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Notifier {
    override fun createNotification(): Notification {
        // 도착 알림 생성
    }
}
```

2. `NotificationsModule`에 바인딩 추가
```kotlin
@Binds
@Named("Arrival")
abstract fun bindsArrivalNotifier(
    notifier: ArrivalNotifier
): Notifier
```

3. 사용
```kotlin
@Inject
@Named("Arrival")
lateinit var arrivalNotifier: Notifier
```

## 의존성

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
}
```

- 순수 Android 모듈 (다른 프로젝트 모듈 의존성 없음)
