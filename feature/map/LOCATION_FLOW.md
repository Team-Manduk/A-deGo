# 📍 위치 정보 흐름 (Location Flow)

## 개요
A-Dego 앱의 위치 정보는 **내 위치**와 **다른 참가자 위치**를 각각 다른 경로로 처리하여 최적의 성능을 제공합니다.

---

## 1. 내 위치 (Current User Location)

### 📥 불러오기: GPS → UI 직접 전달
```
GPS Sensor
  ↓
LocationDataSource.getLocationUpdates()
  ↓
LocationRepository.observeLocationUpdates()
  ↓
MapViewModel.startLocationTracking()
  └─ locationRepository.observeLocationUpdates().collect { location ->
       _uiState.update { it.copy(myCurrentLocation = location) }
     }
  ↓
MapScreen (myCurrentLocation 사용)
  ↓
내 마커 즉시 표시 ⚡ (~32ms)
```

**특징:**
- ⚡ Firebase를 거치지 않고 GPS에서 UI로 직접 전달
- 🔄 5초마다 위치 업데이트
- 📍 가장 최신의 위치를 항상 표시

### 📤 업로드: GPS → Firebase (병렬 처리)
```
GPS Sensor
  ↓
LocationRepository.observeLocationUpdates()
  ↓
BroadcastLocationUseCase (LocationTrackingService에서 실행)
  ↓
UpdateLocationUseCase.invoke(location)
  ├─ shouldUpdate() 체크:
  │   - 첫 업데이트: 무조건 진행
  │   - 30초 경과: 무조건 진행
  │   - 25m 이상 이동: 진행
  │   - 그 외: 스킵 ❌
  ↓
RoomRepository.updateMyLocation()
  ↓
Firebase: participants/{roomId}/{userId}/location 저장
```

**특징:**
- 🚫 불필요한 업로드 차단 (배터리 절약)
- 💾 25m 이상 이동 또는 30초마다 업로드
- 🔄 Foreground Service에서 백그라운드 동작

---

## 2. 다른 참가자 위치 (Other Participants)

### 📥 불러오기 A: 최초 즉시 로드 (1회)
```
MapViewModel.restoreSession()
  ↓
roomRepository.getParticipants(roomId)
  ↓
Firebase: participants/{roomId}.get().await()
  ↓
_uiState.update { participants = ... }
  ↓
다른 참가자 마커 표시 ⚡ (~621ms)
```

**특징:**
- 🎯 최초 1회만 실행
- ⚡ Firebase에서 직접 fetch (빠름)
- 📊 모든 참가자의 최신 위치 포함

### 📥 불러오기 B: 실시간 업데이트 (지속)
```
MapViewModel.restoreSession()
  ↓
JoinRoomUseCase.invoke()
  ↓
observeParticipants() + observeRoom()
  ↓
Firebase: addValueEventListener (실시간 구독)
  ↓
onDataChange() → participants 업데이트
  ↓
_uiState.update { participants = ... }
  ↓
MapScreen 자동 recompose
  ↓
마커 위치 자동 업데이트 🔄
```

**특징:**
- 🔄 실시간 동기화
- 🌐 누군가 이동하면 즉시 반영
- 📡 Firebase ValueEventListener 사용

---

## 3. 전체 아키텍처

```
┌─────────────────────────────────────────┐
│          MapViewModel                   │
│  ┌─────────────┐  ┌──────────────┐     │
│  │myCurrentLoc │  │ participants │     │
│  └──────┬──────┘  └──────┬───────┘     │
└─────────┼─────────────────┼─────────────┘
          │                 │
     GPS 직접           Firebase
          │                 │
┌─────────┴────────┐ ┌──────┴──────────┐
│LocationRepository│ │ RoomRepository  │
│                  │ │                 │
│observeLocation() │ │getParticipants()│(1회)
└──────────────────┘ │observeParticip()│(실시간)
                     └─────────────────┘
```

---

## 4. 시간순 실행 흐름

```
[0ms]    MapViewModel.init()
[10ms]   clearLocationState() + startLocationTracking()
[10ms]   roomRepository.getParticipants() 시작
[32ms]   ⚡ 내 위치 GPS 수신 → UI 즉시 반영
[34ms]   ⚡⚡ MapScreen 렌더링
[88ms]   ⚡⚡⚡ GoogleMap 렌더링
[89ms]   내 위치 Firebase 업로드 시작
[621ms]  ⚡ 다른 참가자 UI 반영
[622ms]  내 위치 Firebase 업로드 완료
[1.4s]   observeParticipants() 첫 데이터
[이후]   실시간 동기화 계속 🔄
```

---

## 5. 성능 최적화 포인트

### ✅ 최적화 전략

1. **내 위치 이중 경로**
   - UI용: GPS → ViewModel (즉시, ~32ms)
   - 공유용: GPS → Firebase (백그라운드, ~622ms)

2. **다른 참가자 이중 로드**
   - 초기: `get()` (즉시, ~621ms)
   - 실시간: `observe()` (지속)

3. **불필요한 업로드 차단**
   - `shouldUpdate()`: 25m 미만 + 30초 미만 = 스킵

4. **백그라운드 위치 추적**
   - Foreground Service 사용
   - 앱 종료해도 위치 공유 계속

---

## 6. 핵심 성능 지표

| 항목 | 시간 | 방법 |
|------|------|------|
| 화면 로딩 | ~34ms | GPS 직접 + get() |
| 내 위치 표시 | ~32ms | GPS → UI 직접 |
| 다른 참가자 표시 | ~621ms | Firebase get() |
| 실시간 동기화 | 항상 활성 | Firebase observe() |

**결과: 1초 이내에 모든 위치 정보 표시 완료!** 🎉

---

## 7. 주요 컴포넌트

### Presentation Layer
- `MapViewModel`: UI 상태 관리, 위치 구독
- `MapScreen`: 지도 + 마커 렌더링

### Domain Layer
- `BroadcastLocationUseCase`: 위치 브로드캐스트
- `UpdateLocationUseCase`: Firebase 업로드 제어
- `JoinRoomUseCase`: 방 참가 + 실시간 구독

### Data Layer
- `LocationRepository`: GPS 위치 관리
- `RoomRepository`: Firebase 참가자 관리

### Service
- `LocationTrackingService`: Foreground Service로 백그라운드 추적

---

## 8. 관련 파일

- `feature/map/src/.../MapViewModel.kt`
- `feature/map/src/.../MapScreen.kt`
- `core/domain/src/.../UpdateLocationUseCase.kt`
- `core/domain/src/.../BroadcastLocationUseCase.kt`
- `sync/location/src/.../LocationTrackingService.kt`
- `core/data/src/.../LocationRepositoryImpl.kt`
- `core/data/src/.../RoomRepositoryImpl.kt`
