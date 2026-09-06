# 10 — API Design

## Style

Spring WebFlux **functional endpoints** only — no `@RestController`,
`@RequestMapping`, `@GetMapping`, etc. (§31, §32, §39.19). Each service
composes its routes in `config/RouterConfig.java`:

```text
RouterConfig
  +-- SiteRouter        GET  /api/sites
  |                     GET  /api/sites/{siteId}
  |                     POST /api/sites/{siteId}/refresh
  +-- DeploymentRouter  GET  /api/deployments
  |                     POST /api/deployments
  |                     GET  /api/deployments/{deploymentId}
  |                     POST /api/deployments/{deploymentId}/retry
  |                     POST /api/deployments/{deploymentId}/cancel
  +-- EvidenceRouter    GET  /api/deployments/{deploymentId}/evidence
  +-- AuditRouter       GET  /api/audit-events
```

Routers only map method+path → handler (`§32`); handlers translate
HTTP↔domain and delegate to `service/`.

## OpenAPI

Each service publishes its contract at
`services/<service>/src/main/resources/openapi/<service>.yaml`
(OpenAPI 3.0). Phase 0 ships the shape of every endpoint listed above
with request/response schemas and the shared error model below; handler
implementations return `501 Not Implemented` until the owning phase.

## Error model (all services, §31)

```json
{
  "timestamp": "2026-01-01T12:00:00Z",
  "status": 409,
  "code": "DEPLOYMENT_ALREADY_EXISTS",
  "message": "A deployment already exists for this request.",
  "correlationId": "..."
}
```

## Idempotency (§12)

`POST /api/deployments` requires an `Idempotency-Key` header. A repeated
request with the same key returns the original `deploymentId` (200) —
not a new deployment (201) — matching `07-sequence-diagrams.md`.

## Pagination & filtering

List endpoints (`GET /api/sites`, `GET /api/deployments`,
`GET /api/audit-events`) accept `page`, `size`, and resource-specific
filters (e.g. `status`, `siteId`), returning a standard envelope:

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 0 }
```

## Correlation

Every response includes/propagates a `correlationId` (request header
`X-Correlation-Id` if supplied, otherwise generated) echoed in the error
model and downstream events (§24).

## Authorization (§6, enforced in Phase 7 — not yet wired)

| Endpoint | VIEWER | OPERATOR | ADMIN |
|---|---|---|---|
| `GET` (read) endpoints | ✔ | ✔ | ✔ |
| `POST /api/deployments`, `/retry`, `/cancel` | ✖ | ✔ | ✔ |
| `POST /api/sites/{id}/refresh` | ✖ | ✔ | ✔ |
| Provider simulation admin endpoints (mocks) | ✖ | ✖ | ✔ |
