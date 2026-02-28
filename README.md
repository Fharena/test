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

`/api/admin/**`는 ADMIN 토큰이 필요합니다. (사전 생성된 admin 계정 로그인 토큰 사용)

```bash
export ADMIN_TOKEN=<admin-jwt>
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
