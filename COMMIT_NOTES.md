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
