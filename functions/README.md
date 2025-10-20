# Adego Firebase Cloud Functions

Adego 앱의 서버 사이드 로직을 담당하는 Firebase Cloud Functions입니다.

## 📖 개요

### Firebase Cloud Functions란?

Firebase 서버에서 자동으로 실행되는 백엔드 코드입니다.
Android 앱에서 특정 이벤트(예: 데이터 삭제)가 발생하면, 서버가 자동으로 추가 작업을 수행합니다.

### 왜 필요한가?

**문제**: 마지막 참가자가 방을 나가면 빈 방이 Firebase에 남아있게 됩니다.

**기존 방식의 문제점**:
- Android 앱에서 "남은 참가자 있나?" 체크 → 복잡하고 Race Condition 발생 가능
- 여러 참가자가 동시에 나가면 충돌 가능

**Cloud Functions 해결**:
- Firebase 서버에서 자동으로 빈 방 정리
- Race Condition 없음 (서버 사이드에서 순차 처리)
- Android 앱 코드는 단순하게 유지

---

## 🔥 핵심 기능

### `onParticipantRemoved` 함수

참가자가 방에서 나갈 때 자동으로 트리거되어 빈 방을 삭제합니다.

#### 동작 원리

```javascript
exports.onParticipantRemoved = functions.database
    .ref("/participants/{roomId}/{userId}")
    .onDelete(async (snapshot, context) => {
      // 1. 경로에서 roomId, userId 자동 추출
      const roomId = context.params.roomId;

      // 2. 해당 방의 남은 참가자 확인
      const participantsSnapshot = await admin
          .database()
          .ref(`/participants/${roomId}`)
          .once("value");

      // 3. 아무도 없으면 방 삭제
      if (!participantsSnapshot.exists() || !participantsSnapshot.hasChildren()) {
          await admin.database().ref(`/rooms/${roomId}`).remove();
      }
    });
```

#### 트리거 조건

Firebase 경로 `/participants/{roomId}/{userId}`에서 데이터가 삭제될 때 자동 실행

**예시**:
```
/participants/ABC123/user789 삭제됨
↓
Cloud Function 자동 실행
↓
roomId = "ABC123", userId = "user789" 추출
```

#### 삭제 판단 로직

```javascript
if (!participantsSnapshot.exists() || !participantsSnapshot.hasChildren())
```

**두 가지 케이스 처리**:

1. **`!exists()`**: 경로 자체가 완전히 삭제된 경우
   ```
   /participants
     └── (ABC123 경로가 아예 없음)
   ```

2. **`!hasChildren()`**: 경로는 있지만 빈 객체인 경우
   ```
   /participants/ABC123: {}  ← 자식 노드 0개
   ```

**OR 연산자(`||`)**: Firebase가 마지막 자식 삭제 시 위 두 가지 중 어떤 동작을 할지 모르기 때문에, 둘 다 체크하여 확실하게 빈 방을 감지합니다.

---

## 📊 실제 동작 시나리오

### 시나리오 1: 3명 중 1명 나가기

```
Before: /participants/ABC123
  ├── user1: { name: "철수" }
  ├── user2: { name: "영희" }
  └── user3: { name: "민수" }

user3 "방 나가기" 클릭
↓
Android: removeParticipant("ABC123", "user3")
↓
Firebase: /participants/ABC123/user3 삭제
↓
Cloud Function 실행:
  - 남은 참가자: user1, user2 (2명)
  - exists() = true, hasChildren() = true
  - 조건: !true || !true = false
  - 결과: 아무것도 안함 (방 유지) ✅

After: /participants/ABC123
  ├── user1: { name: "철수" }
  └── user2: { name: "영희" }
```

**로그**:
```
Participant removed: user3 from room ABC123
2 participant(s) remaining in room ABC123
```

---

### 시나리오 2: 마지막 사람 나가기

```
Before: /participants/ABC123
  └── user1: { name: "철수" }

user1 "방 나가기" 클릭
↓
Android: removeParticipant("ABC123", "user1")
↓
Firebase: /participants/ABC123/user1 삭제
↓
Cloud Function 실행:
  - 남은 참가자: 없음 (0명)
  - exists() = false OR hasChildren() = false
  - 조건: !false || !false = true
  - 결과: /rooms/ABC123 삭제 ✅

After: /participants/ABC123 (삭제됨 또는 빈 객체)
       /rooms/ABC123 (삭제됨)
```

**로그**:
```
Participant removed: user1 from room ABC123
Last participant left. Deleting room: ABC123
Room deleted successfully: ABC123
```

---

## 📁 파일 구조

```
functions/
├── src/
│   ├── room/
│   │   └── onParticipantRemoved.js   # 참가자 제거 시 방 삭제 로직
│   │   # TODO: onRoomCreated.js      # 방 생성 시 로직 (미래)
│   │   # TODO: onRoomExpired.js      # 오래된 방 자동 삭제 (미래)
│   ├── index.js                      # 모든 함수 통합 export
│   # TODO: user/                     # 사용자 관련 함수 (미래)
│   # TODO: notification/             # 알림 관련 함수 (미래)
├── index.js                          # 진입점 (src/index.js로 위임)
├── package.json                      # Node.js 의존성 및 스크립트
├── .eslintrc.js                      # JavaScript 코드 스타일 검사
├── .gitignore                        # Git 제외 파일
└── README.md                         # 이 문서
```

### 각 파일 역할

#### `index.js` (진입점)
Firebase Functions의 진입점입니다. `src/index.js`로 위임하여 실제 로직과 분리합니다.

```javascript
module.exports = require("./src/index");
```

#### `src/index.js` (통합)
모든 기능별 함수를 import하여 통합 export합니다.

```javascript
const onParticipantRemoved = require("./room/onParticipantRemoved");

module.exports = {
  onParticipantRemoved,
  // 추후 추가될 함수들...
};
```

#### `src/room/onParticipantRemoved.js` (핵심 로직)
참가자 제거 시 빈 방을 자동으로 삭제하는 로직이 담긴 파일입니다.

**장점**:
- 기능별로 파일 분리 → 코드 찾기 쉬움
- 새 함수 추가 시 해당 폴더에만 파일 추가
- 팀 협업 시 충돌 최소화

#### `package.json` (의존성)
Node.js 프로젝트 설정 파일로, Android의 `build.gradle`과 유사한 역할입니다.

```json
{
  "dependencies": {
    "firebase-admin": "^12.0.0",      // Firebase 서버 SDK
    "firebase-functions": "^4.5.0"    // Cloud Functions SDK
  }
}
```

#### `.eslintrc.js` (린트)
JavaScript 코드 스타일 검사기 (Android의 ktlint와 유사)

#### `.gitignore`
`node_modules/` 같은 큰 의존성 폴더를 Git에서 제외

---

## 🚀 설정 및 배포

### 1. 사전 요구사항

- **Node.js 18 이상** 설치: [nodejs.org](https://nodejs.org/)
- **Firebase CLI** 설치:
  ```bash
  npm install -g firebase-tools
  ```
- **Firebase 로그인**:
  ```bash
  firebase login
  ```

### 2. 프로젝트 초기화

```bash
# functions 디렉토리로 이동
cd functions

# 의존성 설치
npm install
```

### 3. Firebase 프로젝트 연결 (최초 1회)

```bash
# 프로젝트 루트에서
firebase init functions

# 또는 기존 프로젝트 선택
firebase use --add
```

### 4. 배포

```bash
# functions 디렉토리에서
npm run deploy

# 또는 프로젝트 루트에서
firebase deploy --only functions
```

**배포 후**:
- Firebase Console > Functions 메뉴에서 배포된 함수 확인
- `onParticipantRemoved` 함수가 "활성" 상태인지 확인

### 5. 로그 확인

```bash
# 실시간 로그 스트림
firebase functions:log

# 또는
cd functions
npm run logs
```

---

## 🧪 로컬 테스트 (선택)

배포 전에 로컬에서 테스트할 수 있습니다.

```bash
cd functions

# Firebase Emulator 실행
npm run serve

# Functions Shell 실행
npm run shell
```

---

## 💰 비용 안내

### 무료 할당량 (Spark 플랜)

Firebase Cloud Functions는 다음 무료 할당량을 제공합니다:

| 항목 | 무료 한도 |
|------|-----------|
| 호출 횟수 | 200만 회/월 |
| 컴퓨팅 시간 | 40만 GB-초/월 |
| 네트워크 | 5GB/월 |

### Adego 사용 패턴 분석

`onParticipantRemoved` 함수는:
- **트리거**: 참가자가 방을 나갈 때만 실행
- **실행 시간**: 약 0.1초 미만 (단순 read + delete)
- **예상 호출**: 하루 100명이 10번씩 나가도 → 1,000회/일 = 30,000회/월

**결론**: 무료 범위(200만 회) 내에서 충분히 사용 가능합니다! ✅

자세한 내용: [Firebase 요금제](https://firebase.google.com/pricing)

---

## 🔧 문제 해결

### 배포 실패 시

```bash
# Firebase CLI 업데이트
npm install -g firebase-tools@latest

# 프로젝트 재설정
firebase use --add

# package-lock.json 삭제 후 재설치
cd functions
rm -rf node_modules package-lock.json
npm install
```

### 함수가 트리거되지 않을 때

1. **Firebase Console 확인**
   - Functions 메뉴에서 `onParticipantRemoved` 배포 상태 확인
   - 에러 로그 확인

2. **Realtime Database 규칙 확인**
   - 읽기/쓰기 권한이 올바른지 확인
   - Functions는 admin 권한으로 동작하므로 대부분 문제없음

3. **로그 확인**
   ```bash
   firebase functions:log
   ```
   - "Participant removed" 로그가 출력되는지 확인

4. **Android 앱 확인**
   - `removeParticipant()` 호출이 정상적으로 되는지 확인
   - Firebase Database에서 실제로 삭제되는지 확인

---

## 📚 개발 가이드

### 새 함수 추가

**1. 기능별 폴더에 함수 파일 생성**

예시: `src/room/onRoomCreated.js`

```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");

/**
 * 방 생성 시 자동 처리
 */
module.exports = functions.database
  .ref('/rooms/{roomId}')
  .onCreate(async (snapshot, context) => {
    const roomId = context.params.roomId;
    const roomData = snapshot.val();

    console.log(`New room created: ${roomId}`);
    // 추가 로직...
  });
```

**2. `src/index.js`에 함수 추가**

```javascript
const onParticipantRemoved = require("./room/onParticipantRemoved");
const onRoomCreated = require("./room/onRoomCreated");  // 추가

module.exports = {
  onParticipantRemoved,
  onRoomCreated,  // 추가
};
```

**완료!** 배포하면 자동으로 인식됩니다.

### 사용 가능한 트리거

```javascript
// 데이터 생성 시
.onCreate()

// 데이터 삭제 시 (현재 사용 중)
.onDelete()

// 데이터 업데이트 시
.onUpdate()

// 생성/업데이트/삭제 모두
.onWrite()
```

### 테스트 코드 작성

```bash
npm install --save-dev mocha chai
```

`test/` 디렉토리에 테스트 파일 작성:

```javascript
const assert = require('assert');
const functions = require('../index');

describe('Cloud Functions', () => {
  it('should delete room when last participant leaves', async () => {
    // 테스트 로직
  });
});
```

실행:
```bash
npm test
```

---

## 🔗 참고 자료

- [Firebase Cloud Functions 공식 문서](https://firebase.google.com/docs/functions)
- [Firebase Realtime Database Triggers](https://firebase.google.com/docs/functions/database-events)
- [Firebase Functions 샘플](https://github.com/firebase/functions-samples)
- [Node.js Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)

---

## ✅ 체크리스트

배포 전 확인사항:

- [ ] Node.js 18 이상 설치 완료
- [ ] Firebase CLI 설치 및 로그인 완료
- [ ] `npm install` 실행 완료
- [ ] Firebase 프로젝트 연결 완료 (`firebase use`)
- [ ] `npm run deploy` 배포 성공
- [ ] Firebase Console에서 함수 확인
- [ ] Android 앱에서 "방 나가기" 테스트
- [ ] Firebase Database에서 빈 방이 자동 삭제되는지 확인

---

## 📝 요약

**핵심 로직**: `index.js` 파일 하나에 담긴 `onParticipantRemoved` 함수

**동작**:
1. 참가자가 방에서 나감 (Android 앱에서 `removeParticipant()` 호출)
2. Firebase에서 `/participants/{roomId}/{userId}` 삭제
3. Cloud Function 자동 트리거
4. 남은 참가자 확인
5. 마지막 참가자였으면 → `/rooms/{roomId}` 삭제

**결과**: 빈 방이 Firebase에 남지 않고 자동으로 정리됩니다! ✨
