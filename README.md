# A-deGo

> **A**rrival **De**tection for **Go**ing - 실시간 위치 공유 및 모임 조율 안드로이드 앱

A-deGo는 여러 사람이 약속 장소에 도착하는 과정을 실시간으로 공유하고 관리할 수 있는 안드로이드 애플리케이션입니다.

## 주요 기능

- **방 생성 및 참여**: 초대 코드로 간편하게 모임방 생성 및 참여
- **실시간 위치 추적**: GPS 기반 백그라운드 위치 추적 및 실시간 동기화
- **대중교통 경로 검색**: TMAP API를 활용한 한국 대중교통 경로 제공
- **참가자 시각화**: 지도에서 모든 참가자의 위치를 실시간으로 확인
- **자동 상태 감지**: 참가자의 이동 상태 자동 감지 (미출발, 준비, 이동중, 도착임박, 도착)
- **세션 관리**: 앱 종료 후 재실행 시 자동 세션 복원

## 기술 스택

### Core
- **Kotlin 2.1.10** - 주 언어
- **Jetpack Compose** - 선언형 UI
- **Material 3** - 디자인 시스템
- **Hilt** - 의존성 주입
- **Coroutines & Flow** - 비동기 처리 및 리액티브 스트림

### Backend & Data
- **Firebase Realtime Database** - 실시간 데이터 동기화
- **Firebase Authentication** - 사용자 인증
- **Room Database** - 로컬 데이터 저장
- **DataStore** - 환경설정 저장

### Location & Maps
- **Google Maps Compose** - 지도 UI
- **Fused Location Provider** - 위치 추적
- **TMAP API** - 대중교통 경로 검색

### Networking
- **Ktor Client** - HTTP 통신
- **Kotlinx Serialization** - JSON 직렬화

## 빠른 시작

### 환경 요구사항

- Android Studio Ladybug or later
- JDK 11+
- Android SDK 36
- Minimum SDK 26 (Android 8.0)

### API 키 설정

1. `secrets.properties` 파일을 프로젝트 루트에 생성:
```properties
MAPS_API_KEY=your_google_maps_api_key
TMAP_API_KEY=your_tmap_api_key
```

2. Firebase 프로젝트 설정:
   - Firebase Console에서 프로젝트 생성
   - `google-services.json` 파일을 `app/` 디렉토리에 추가
   - Realtime Database 활성화

### 빌드 및 실행

```bash
# 프로젝트 클론
git clone https://github.com/Team-Manduk/A-deGo.git
cd A-deGo

# 빌드
./gradlew build

# 디버그 APK 설치
./gradlew installDebug
```

## 프로젝트 구조

A-deGo는 Clean Architecture와 멀티 모듈 구조를 따릅니다:

```
A-deGo/
├── app/                          # 애플리케이션 진입점
├── feature/                      # Feature 모듈
│   ├── main/                     # [메인 네비게이션 및 세션 관리](feature/main/README.md)
│   ├── home/                     # [홈 화면](feature/home/README.md)
│   ├── create/                   # [방 생성](feature/create/README.md)
│   ├── map/                      # [지도 및 참가자 추적](feature/map/README.md)
│   ├── place/                    # [장소 선택](feature/place/README.md)
│   └── route/                    # [경로 선택](feature/route/README.md)
├── core/                         # Core 모듈
│   ├── common/                   # 공통 유틸리티
│   ├── designsystem/             # 디자인 시스템
│   ├── domain/                   # Domain 레이어 (UseCase, Repository 인터페이스)
│   ├── data/                     # Data 레이어 (Repository 구현)
│   ├── data-api/                 # DataSource 인터페이스 및 DTO
│   ├── local/                    # 로컬 데이터 소스 (Room, DataStore)
│   ├── remote/                   # 원격 데이터 소스 (Firebase, API)
│   ├── model/                    # 도메인 모델
│   ├── navigation/               # 네비게이션 정의
│   ├── notifications/            # 알림 관리
│   ├── ui/                       # 공유 UI 컴포넌트
│   └── tmap/                     # TMAP API 통합
├── sync/                         # 백그라운드 작업
│   └── location/                 # [위치 추적 서비스](sync/location/README.md)
└── build-logic/                  # 빌드 설정 컨벤션 플러그인
```

## 아키텍처

A-deGo는 **Clean Architecture** 원칙을 따릅니다:

```
┌─────────────────────────────────────┐
│   Presentation Layer                │
│   - Jetpack Compose UI              │
│   - ViewModels (MVI Pattern)        │
│   - Navigation                      │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   Domain Layer                      │
│   - Use Cases                       │
│   - Repository Interfaces           │
│   - Domain Models                   │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   Data Layer                        │
│   - Repository Implementations      │
│   - Data Sources (Local, Remote)    │
│   - DTOs & Mappers                  │
└─────────────────────────────────────┘
```

### 주요 패턴

- **MVI (Model-View-Intent)**: UI 상태 관리
- **Repository Pattern**: 데이터 소스 추상화
- **UseCase Pattern**: 비즈니스 로직 캡슐화
- **Mapper Pattern**: 계층 간 데이터 변환
- **Dependency Injection**: Hilt를 통한 의존성 관리

## 주요 기능 플로우

### 1. 위치 추적 (Dual-Path 최적화)

A-deGo는 성능 최적화를 위해 이중 경로 위치 업데이트를 사용합니다:

- **빠른 경로** (UI): GPS → UI 직접 업데이트 (~32ms)
- **느린 경로** (Firebase): GPS → 필터링 → Firebase 업데이트 (~600ms)

상세 내용: [feature/map/LOCATION_FLOW.md](feature/map/LOCATION_FLOW.md)

### 2. 실시간 참가자 동기화

Firebase Realtime Database를 통한 실시간 참가자 위치 및 상태 동기화:
- ValueEventListener를 통한 자동 업데이트
- Flow 기반 리액티브 UI 업데이트

### 3. 이동 상태 자동 감지

Haversine 공식을 사용한 거리 기반 상태 계산:
- **ARRIVED** (≤100m)
- **ARRIVING_SOON** (≤500m)
- **IN_PROGRESS** (이동 중)
- **READY** (≤200m, 경로 선택됨)
- **NOT_STARTED** (경로 미선택)

## 문서

### Feature 문서
- [Main - 메인 네비게이션](feature/main/README.md)
- [Home - 홈 화면](feature/home/README.md)
- [Create - 방 생성](feature/create/README.md)
- [Map - 지도 및 추적](feature/map/README.md)
- [Place - 장소 선택](feature/place/README.md)
- [Route - 경로 선택](feature/route/README.md)

### 기술 문서
- [위치 추적 플로우](feature/map/LOCATION_FLOW.md)
- [위치 추적 서비스](sync/location/README.md)
- [알림 시스템](core/notifications/README.md)

## 개발 가이드

### 코드 컨벤션

- Kotlin 공식 코딩 컨벤션 준수
- Compose 컴포넌트는 PascalCase
- ViewModel은 MVI 패턴 사용
- UseCase는 단일 책임 원칙 준수

### 브랜치 전략

- `main`: 프로덕션 릴리스
- `dev`: 개발 통합 브랜치
- `feature/*`: 기능 개발
- `bugfix/*`: 버그 수정

### 커밋 메시지

```
<type>: <subject>

<body>
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

## 라이선스

이 프로젝트는 Team Manduk의 소유입니다.

## 팀

**Team Manduk** - A-deGo Development Team

---

**Built with ❤️ by Team Manduk**
