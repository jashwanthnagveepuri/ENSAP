# site-profile-service

Owns site/device/network-profile/WAN-circuit/VLAN/subnet data
(`../../docs/06-component-design.md`). Business logic (search, refresh
against mock source systems, Redis caching) lands in **Phase 1**
(`../../docs/02-functional-requirements.md`, FR-1) — today every
endpoint returns `501 Not Implemented` with the shared error shape.

## Run locally

```bash
./mvnw spring-boot:run
```

Requires PostgreSQL reachable at `SPRING_DATASOURCE_URL` (defaults to
`localhost:5432/ensap`, matching `infrastructure/docker/docker-compose.yml`)
and Redis at `SPRING_DATA_REDIS_HOST` (defaults to `localhost`).

## Build & test

```bash
./mvnw -q verify
```

Runs the Phase 0 Spring context smoke test against an in-memory H2
database (`src/test/resources/application-test.yml`) — no external
services required to run tests.

## API contract

`src/main/resources/openapi/site-profile-service.yaml`

## Package layout

Functional endpoints per master spec §31/§32 — see
`../../docs/06-component-design.md` for the shared layering across all
three services (`config/router/handler/service/repository/entity/dto/
exception`).
