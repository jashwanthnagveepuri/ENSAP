# 02 — Functional Requirements

Requirements are grouped by owning service (see `06-component-design.md`
for service boundaries). Each requirement notes the phase it targets
(see `README.md` for phase status); Phase 0 delivers none of the
business behavior below — only the API/data/event shapes it will use.

## FR-1 Site Profile (Site Profile Service) — Phase 1

- FR-1.1 Create/import a synthetic site profile from a mock location
  source.
- FR-1.2 Search sites by id, name, region, status.
- FR-1.3 View a site's devices, network profile, WAN circuits, VLANs,
  subnets.
- FR-1.4 Refresh a site profile from mock source systems and record
  source freshness / reconciliation metadata.

## FR-2 Deployment (Deployment Service) — Phase 2+

- FR-2.1 Start a deployment for a site (single) or a batch of sites
  (Phase 6), guarded by an `Idempotency-Key`.
- FR-2.2 Track deployment status and per-step status
  (`PENDING/IN_PROGRESS/SUCCEEDED/FAILED/FAILED_REQUIRES_ATTENTION/
  CANCELLED`).
- FR-2.3 Request the Camunda workflow that orchestrates provisioning
  steps.
- FR-2.4 Allow an operator to manually retry, resume, or cancel a
  deployment or a single failed step.

## FR-3 Provider Workers — Phase 4+

- FR-3.1 Each worker (router/switch/wireless/firewall/ticketing)
  consumes its provisioning-requested event, calls its mock provider API,
  and publishes a completed/failed event.
- FR-3.2 Workers are idempotent: replaying the same `operationId` must
  not re-execute a completed operation.
- FR-3.3 Workers apply provider-specific concurrency limits, timeouts,
  and retry/circuit-breaker policies (Phase 5).

## FR-4 Evidence & Audit (Evidence/Audit Service) — Phase 2+

- FR-4.1 Store per-step evidence (provider request/response, validation
  reports) with large payloads in S3 and metadata in PostgreSQL.
- FR-4.2 Record an immutable audit event for every meaningful state
  transition, actor, and timestamp.
- FR-4.3 Expose audit history and evidence for a deployment.

## FR-5 Provider Simulation / Admin — Phase 4+

- FR-5.1 Admin can configure a mock provider's behavior at runtime:
  success rate, fixed/random latency, specific HTTP error codes,
  timeout, temporary outage, duplicate callback, rate limiting.

## FR-6 Frontend (Operator Console) — Phase 1+ (pages added per phase)

The 13 pages listed in `06-component-design.md` / master spec §5. Each
page is added when its backing API exists; Phase 0 creates route stubs
only (see `frontend/README.md`).

## FR-7 AuthN/AuthZ — Phase 7 (local JWT dev-mode earlier)

- FR-7.1 Every mutating API call is authorized server-side by role
  (`VIEWER`/`OPERATOR`/`ADMIN`) — the frontend is never the sole
  enforcement point.
- FR-7.2 Local development uses a JWT-compatible mode; production swaps
  in Cognito without changing service-side authorization code.
