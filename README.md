# Attendance Checker Server

출석 체크를 위한 Google Forms 기반 서버 애플리케이션

## 설정 방법

### 1. Google Cloud Console 설정

1. [Google Cloud Console](https://console.cloud.google.com)에서 새 프로젝트를 생성합니다.
2. Google Forms API를 활성화합니다:
   - APIs & Services > Library로 이동
   - "Google Forms API" 검색 후 활성화

### 2. OAuth 2.0 클라이언트 ID 설정

1. APIs & Services > Credentials로 이동
2. Create Credentials > OAuth client ID 선택
3. Application type을 "Desktop app"으로 선택
4. 생성된 클라이언트 ID의 JSON 파일을 다운로드
5. 다운로드한 파일을 프로젝트 루트 디렉토리에 `credentials.json`으로 저장

### 3. 프로젝트 실행

```bash
./gradlew run
```

첫 실행 시 Google 계정 인증이 필요합니다. 브라우저에서 인증을 완료하면 토큰이 자동으로 저장됩니다.

## 주의사항

- `credentials.json`과 `tokens/` 디렉토리는 민감한 정보를 포함하고 있으므로 절대 Git에 커밋하지 마세요.
- 각 개발자는 자신의 Google Cloud Console 프로젝트에서 발급받은 credentials를 사용해야 합니다. 