# AI Chat API (MVP)

Spring Boot 3 + Kotlin 1.9 기반 AI 챗봇 API 서버 과제용 프로젝트입니다.

## Prerequisites

- JDK 17
- Gradle (또는 Gradle Wrapper)
- Docker (prod DB 로컬 실행 시)

## Profiles

- `dev`: H2 in-memory DB, `MockAiProvider` 사용 전제
- `prod`: PostgreSQL, OpenAI 호출 구현(`WebClient`) 전제

## Run (dev)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

기본값이 `dev`이므로 프로필 인자 없이도 실행 가능합니다.

### Dev 기본 관리자 계정(자동 생성)

`dev` 실행 시 아래 계정이 자동으로 1회 생성됩니다(이미 있으면 재생성하지 않음).

- email: `admin@example.com`
- password: `Admin123!`

필요하면 환경변수로 변경할 수 있습니다.

```bash
export BOOTSTRAP_ADMIN_ENABLED=true
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com
export BOOTSTRAP_ADMIN_PASSWORD=Admin123!
export BOOTSTRAP_ADMIN_NAME="Demo Admin"
```

## Run (prod)

1. PostgreSQL 실행

```bash
docker compose up -d postgres
```

2. 환경 변수 설정

```bash
export DB_URL=jdbc:postgresql://localhost:5432/aichat
export DB_USER=aichat
export DB_PASSWORD=aichat
export JWT_SECRET=replace-with-strong-secret
export OPENAI_API_KEY=your-key
```

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/aichat"
$env:DB_USER="aichat"
$env:DB_PASSWORD="aichat"
$env:JWT_SECRET="replace-with-strong-secret"
$env:OPENAI_API_KEY="your-key"
```

3. prod 프로필로 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 시연용 curl 시나리오

아래 예시는 `dev` 또는 `test` 프로필(= `MockAiProvider`) 기준입니다. OpenAI API 키 없이 동작합니다.

### 0) 공통 변수

```bash
export BASE_URL=http://localhost:8080
```

Windows PowerShell:

```powershell
$env:BASE_URL="http://localhost:8080"
```

### 1) Signup

```bash
curl -i -X POST "$BASE_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email":"member1@example.com",
    "password":"Password123!",
    "name":"Member One"
  }'
```

### 2) Login + TOKEN 저장

```bash
export TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"member1@example.com","password":"Password123!"}' | jq -r '.accessToken')
echo "$TOKEN"
```

Windows PowerShell:

```powershell
$env:TOKEN = (curl -s -X POST "$env:BASE_URL/api/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"email":"member1@example.com","password":"Password123!"}' | ConvertFrom-Json).accessToken
$env:TOKEN
```

### 3) Chat 생성

```bash
CHAT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/chats" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"안녕, MVP 테스트야","isStreaming":false}')
echo "$CHAT_RESPONSE"
```

`chatId` 추출:

```bash
export CHAT_ID=$(echo "$CHAT_RESPONSE" | jq -r '.chatId')
echo "$CHAT_ID"
```

### 4) Threads 조회

```bash
curl -s "$BASE_URL/api/threads?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

### 5) Feedback 생성/조회

```bash
curl -i -X POST "$BASE_URL/api/feedbacks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"chatId\":\"$CHAT_ID\",\"isPositive\":true}"
```

```bash
curl -s "$BASE_URL/api/feedbacks?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

### 6) Admin metrics / CSV

`/api/admin/**`는 ADMIN 토큰이 필요합니다. (`dev`에서는 bootstrap admin으로 로그인)

```bash
export ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}' | jq -r '.accessToken')
```

```bash
curl -s "$BASE_URL/api/admin/metrics/daily" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

```bash
curl -L "$BASE_URL/api/admin/reports/daily-chats.csv" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -o daily-chats.csv
```

## Notes

- `prod`에서는 `spring.jpa.hibernate.ddl-auto=validate`로 설정되어 있습니다.
- OpenAI 연동은 `AiProvider` 구현 작업 시 `prod` 프로필에서만 활성화되도록 구성하는 것을 권장합니다.

## 과제 진행 메모

### 1) 과제를 어떻게 분석했는가

이번 과제는 3시간 제한이 있어서, 먼저 "필수 흐름이 끝까지 동작하는지"를 기준으로 우선순위를 정했습니다.  
요구사항을 인증/권한, 채팅/스레드, 피드백, 관리자 기능, 테스트/문서로 나눠서 앞 기능이 뒤 기능의 전제가 되도록 순서대로 구현했습니다.

처음부터 모든 걸 정교하게 만들기보다는, 회원가입-로그인-JWT 인증-채팅 생성까지 먼저 연결하고 이후 세부 조건(권한, 페이징/정렬, 상태 변경, 리포트)을 붙이는 방식으로 진행했습니다.  
또한 과제 요구사항에 맞게 dev/test에서는 API 키 없이 동작하고, prod에서는 실제 AI 연동이 가능하도록 provider를 분리하는 구조를 우선 설계했습니다.

### 2) 과제 진행에서 AI를 어떻게 활용했는가, 어떤 어려움이 있었는가

이번 스택을 깊게 써본 경험이 많지 않아서, AI는 학습과 구현 속도를 높이는 보조 도구로 사용했습니다.  
주로 코드 초안 생성, 테스트 케이스 초안 정리, 빠른 비교/검토에 활용했고, 최종 반영은 직접 실행과 테스트 결과를 기준으로 결정했습니다.

권한 규칙, 예외 응답 형식, 실제 API 응답 같은 부분은 직접 요청을 보내고 로그를 확인하면서 수정했습니다.

### 3) 가장 어려웠던 기능(1개 이상)

가장 어려웠던 부분은 채팅 생성 로직이었습니다.  
한 요청 안에서 스레드 30분 규칙, 과거 대화 히스토리 구성, AI 응답 생성, DB 저장, 활동 로그 기록까지 동시에 맞춰야 해서 실수 여지가 많았습니다.  
특히 스트리밍 응답일 때도 최종 답변을 정상 저장해야 해서, 일반 응답과 저장 로직을 일관되게 맞추는 데 신경을 많이 썼습니다.

추가로 관리자 CSV 리포트도 생각보다 까다로웠습니다.  
질문/답변에 쉼표, 줄바꿈, 따옴표가 들어갈 수 있어 escaping 처리가 필요했고, 실제 시연 환경에서 한글 인코딩 문제도 확인되어 UTF-8 BOM/charset 처리까지 반영했습니다.
