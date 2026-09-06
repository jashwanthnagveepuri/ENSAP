# deployment-service

Owns deployment/batch/step lifecycle, idempotency keys, and the outbox
(`../../docs/06-component-design.md`). Business logic (idempotent
create, Camunda `deployment-process` orchestration, retry/cancel) lands
in **Phase 2/5** (`../../docs/02-functional-requirements.md`, FR-2) —
today every endpoint returns `501 Not Implemented` with the shared
error shape.

## Run locally

```bash
./mvnw spring-boot:run
```

Requires PostgreSQL reachable at `SPRING_DATASOURCE_URL` (defaults to
`localhost:5432/ensap`, matching `infrastructure/docker/docker-compose.yml`).

## Build & test

```bash
./mvnw -q verify
```

Runs the Phase 0 Spring context smoke test against an in-memory H2
database (`src/test/resources/application-test.yml`).

## API contract

`src/main/resources/openapi/deployment-service.yaml`

## Package layout

Functional endpoints per master spec §31/§32 — see
`../../docs/06-component-design.md` for the shared layering across all
three services.
