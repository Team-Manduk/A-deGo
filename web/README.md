# A-dego Web - Firebase Hosting 설정 가이드

이 폴더는 `https://adego.kr` 딥링크를 위한 웹 호스팅 설정입니다.

## 📋 설정 순서

### 1. SHA256 Fingerprint 얻기

#### 방법 1: Android Studio 사용 (추천 - 가장 쉬움)

1. Android Studio에서 프로젝트 열기
2. 오른쪽 **Gradle** 탭 클릭
3. **Tasks > android > signingReport** 더블클릭 실행
4. Run 창에서 출력된 **SHA-256** 값 복사

```
Variant: debug
Config: debug
Store: ~/.android/debug.keystore
Alias: AndroidDebugKey
MD5: XX:XX:XX:...
SHA1: XX:XX:XX:...
SHA-256: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:... (← 이 값 복사)
```

#### 방법 2: 터미널 사용 (keytool)

**Debug 키:**
```bash
# Windows
"C:/Program Files/Android/Android Studio/jbr/bin/keytool.exe" -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Release 키:**
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-alias
```

출력에서 `SHA256:` 뒤의 값을 복사하세요.

### 2. assetlinks.json 업데이트

`public/.well-known/assetlinks.json` 파일을 열고:
- `REPLACE_WITH_YOUR_DEBUG_KEY_SHA256`를 위에서 얻은 Debug 키 SHA256으로 교체
- `REPLACE_WITH_YOUR_RELEASE_KEY_SHA256`를 Release 키 SHA256으로 교체

예시:
```json
"sha256_cert_fingerprints": [
  "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
]
```

### 3. Firebase CLI 설치 및 로그인

```bash
npm install -g firebase-tools
firebase login
```

### 4. Firebase 프로젝트 연결

```bash
cd web
firebase init hosting
```

- 기존 Firebase 프로젝트 선택 (a-dego 프로젝트)
- Public directory: `public` 입력
- Single-page app: `No` 입력
- GitHub 자동 배포: 필요시 설정

### 5. Firebase Hosting 배포

```bash
firebase deploy --only hosting
```

배포 후 제공되는 URL (예: `https://a-dego.web.app`)을 확인하세요.

### 6. 가비아 DNS 설정

1. [가비아 My가비아](https://www.gabia.com/) 로그인
2. **서비스 관리 > 도메인** 클릭
3. `adego.kr` 도메인의 **관리** 클릭
4. **DNS 정보** 클릭
5. 다음 레코드 추가:

#### Firebase Hosting 커스텀 도메인 설정

Firebase Console에서:
1. **Hosting > 도메인 추가**
2. `adego.kr` 입력
3. Firebase가 제공하는 TXT/A 레코드를 가비아 DNS에 추가

예시:
```
타입: A
호스트: @
값: [Firebase가 제공하는 IP]
TTL: 3600

타입: TXT
호스트: @
값: [Firebase 검증 코드]
TTL: 3600
```

### 7. 딥링크 테스트

#### ADB로 테스트
```bash
adb shell am start -W -a android.intent.action.VIEW -d "https://adego.kr/join/TEST_ROOM_123" com.teammanduk.adego
```

#### 실제 테스트
1. 앱 빌드 및 설치
2. 문자나 카카오톡으로 초대 링크 공유
3. 링크 클릭 시 앱이 자동으로 열리는지 확인

## 📁 파일 구조

```
web/
├── firebase.json              # Firebase Hosting 설정
├── public/
│   ├── index.html            # 메인 페이지
│   ├── join.html             # 초대 리다이렉트 페이지
│   └── .well-known/
│       └── assetlinks.json   # Android App Links 검증 파일
└── README.md                 # 이 파일
```

## 🔍 문제 해결

### 앱이 자동으로 열리지 않을 때
1. `assetlinks.json`의 SHA256이 올바른지 확인
2. `https://adego.kr/.well-known/assetlinks.json`이 접근 가능한지 확인
3. DNS 전파 대기 (최대 24-48시간)
4. 앱 재설치 후 테스트

### DNS 전파 확인
```bash
nslookup adego.kr
```

또는 https://dnschecker.org 에서 확인

## 📱 동작 방식

1. **앱 설치됨**:
   - `https://adego.kr/join/ABC123` 클릭
   - Android가 자동으로 앱 실행 (브라우저 안 열림)

2. **앱 미설치**:
   - `https://adego.kr/join/ABC123` 클릭
   - 웹 페이지가 열리고 2.5초 후 Play Store로 이동
