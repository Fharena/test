# Commit Notes

## 2026-02-28 - feature/feedback-api

- Scope:
  - Implemented feedback API (`POST /api/feedbacks`, `GET /api/feedbacks`, `PATCH /api/feedbacks/{id}/status`)
  - Added role-based access checks (member/admin) and status update admin-only logic
  - Added pagination/sort and `isPositive` filter support
  - Added integration tests for permission and filtering behavior

- Issues found:
  - Security integration test used a PATCH route that could collide with the real feedback controller mapping.
  - Duplicate feedback race condition could still occur at DB unique constraint boundary.

- Resolutions:
  - Kept test-only fallback route matching (`/api/feedback/*/status`) in security matcher while enforcing real route (`/api/feedbacks/*/status`).
  - Added `DataIntegrityViolationException` handling in feedback create path to return `409 CONFLICT` consistently.

- Verification:
  - `.\gradlew.bat test` passed successfully.

## 2026-02-28 - feature/admin-api

- Scope:
  - Implemented admin API endpoints:
    - `GET /api/admin/metrics/daily`
    - `GET /api/admin/reports/daily-chats.csv`
  - Added 24-hour activity aggregation for `SIGNUP`, `LOGIN`, `CHAT_CREATE`
  - Added CSV report generation including user/thread/chat data for recent chats
  - Added admin integration tests for authz, metrics counts, and CSV output/escaping

- Issues found:
  - CSV generation can break when `question/answer` contains comma, quote, or line breaks.
  - Report query could trigger lazy loading overhead if thread/user are loaded row by row.

- Resolutions:
  - Implemented explicit CSV escaping (`""` quote escaping + field wrapping on comma/quote/newline).
  - Added repository query with `join fetch` for `chat -> thread -> user` in a single read path.

- Verification:
  - `.\gradlew.bat test --tests com.example.aichat.admin.AdminControllerIntegrationTest` passed.
  - `.\gradlew.bat test` passed.
