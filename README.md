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
gradle bootRun --args='--spring.profiles.active=dev'
```

Windows PowerShell:

```powershell
gradle bootRun --args="--spring.profiles.active=dev"
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
gradle bootRun --args='--spring.profiles.active=prod'
```

## Notes

- `prod`에서는 `spring.jpa.hibernate.ddl-auto=validate`로 설정되어 있습니다.
- OpenAI 연동은 `AiProvider` 구현 작업 시 `prod` 프로필에서만 활성화되도록 구성하는 것을 권장합니다.
- 필요 시 `gradle wrapper`를 실행해 `gradlew`/`gradlew.bat`를 생성해 사용할 수 있습니다.
