# 12 — Security (target design; implemented in Phase 7)

## AuthN/AuthZ (§6)

- Production: AWS Cognito User Pool, OAuth2/OIDC, JWT access tokens,
  group/claim-based RBAC.
- Local dev: a JWT-compatible local mode (e.g. a dev-only issuer) so the
  app runs with no AWS credentials, using the **same** authorization
  model/claims shape as Cognito, so swapping providers doesn't touch
  service-side authorization code.
- Roles: `VIEWER`, `OPERATOR`, `ADMIN` (permission matrix in
  `10-api-design.md`).
- The backend re-validates the JWT and enforces RBAC on every mutating
  endpoint; the frontend hiding a button is never sufficient (§5, §6).

## Secrets (§20)

- AWS Secrets Manager for runtime secrets (DB credentials, API keys);
  AWS KMS for encryption at rest.
- No secrets committed to the repo, no credentials baked into Docker
  images. Local Compose uses non-secret placeholder dev credentials only
  (documented in `infrastructure/docker/docker-compose.yml`), which are
  explicitly not suitable for anything but a laptop.
- `.gitignore` excludes local `.env` files.

## Network

- IAM least privilege; EKS Pod Identity/IRSA where appropriate (later
  phase).
- TLS for external traffic.
- PostgreSQL/Redis/Kafka(MSK) never exposed publicly without a
  deliberate, documented reason.
- Kubernetes NetworkPolicies restrict pod-to-pod traffic (Phase 9).

## Audit

- Every meaningful state transition produces an `audit_event` row
  (Evidence/Audit Service) capturing actor, event type, and details —
  this is the operational audit trail, independent of Kafka event
  transport.

## Never (project-wide rule, §20/§39.4)

Hard-coded credentials, committed secrets, AWS access keys in images,
invented real vendor credentials/API keys/infrastructure IDs, or public
exposure of data services without a documented reason.

## Phase 0 status

No authentication is wired into any service yet — endpoints are
unauthenticated `501 Not Implemented` stubs. This document defines the
target model that Phase 7 implements.
