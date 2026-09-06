# evidence-audit-service

Owns deployment evidence metadata, audit events, and processed-operation
records (`../../docs/06-component-design.md`). Evidence bodies live in
S3 (`../../docs/18-adr/0007-s3-for-evidence-and-artifacts.md`); business
logic lands in **Phase 2** (`../../docs/02-functional-requirements.md`,
FR-4) — today every endpoint returns `501 Not Implemented` with the
shared error shape.

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

`src/main/resources/openapi/evidence-audit-service.yaml`

## Package layout

Functional endpoints per master spec §31/§32 — two resource routers
(`router/EvidenceRouter`, `router/AuditRouter`), see
`../../docs/06-component-design.md` for the shared layering across all
three services.
