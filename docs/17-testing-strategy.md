# 17 — Testing Strategy

## Layers (§29)

| Layer | Tooling | Status |
|---|---|---|
| Unit | JUnit 5, Mockito | Phase 0: one context-load smoke test per service |
| Integration | Testcontainers (PostgreSQL, Kafka) | Not yet — Phase 1+ |
| External API contracts | WireMock | Not yet — Phase 4 |
| API/E2E | Karate (or equivalent) | Not yet — Phase 1+ |
| Frontend | React Testing Library, Vitest | Phase 0: one render smoke test |

## Distributed-system failure scenarios to cover explicitly (§29)

These are **test scenarios to write**, not yet implemented — tracked
here so later phases don't quietly skip them:

1. Successful deployment
2. Provider timeout
3. Provider 500
4. Provider 503
5. Provider 429 (respecting backoff)
6. Retry exhaustion → `FAILED_REQUIRES_ATTENTION`
7. Duplicate Kafka message (idempotent consumer)
8. Duplicate API request (idempotency key)
9. Worker restart mid-operation
10. Service restart mid-workflow
11. Manual retry
12. Manual cancellation
13. Partial deployment failure (some steps succeed, one fails)
14. Recovery after temporary provider outage
15. Kafka consumer lag
16. Database failure/recovery

The goal is demonstrating distributed-system failure handling, not just
happy-path unit tests (§29).

## Phase 0 verification

What actually runs today (see the Phase 0 report in the top-level
README for real command output):

- `./mvnw -q verify` per service — compiles and runs the one context
  smoke test in `src/test/java`.
- `npm run build` and `npm test` in `frontend/` — TypeScript build and
  one React Testing Library smoke test.

No benchmark numbers are claimed anywhere in this repository unless a
command was actually run and its output recorded (§39.14).
