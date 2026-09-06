# ADR-0010: API Gateway + internal ALB for the API layer

## Status
Accepted (design-only; implemented in Phase 10)

## Context
Browser traffic needs an edge layer (throttling, request validation,
Cognito authorizer integration) before it reaches EKS-hosted services,
while service-to-service/internal routing within the VPC needs simple,
cheap L7 load balancing (§17).

## Decision
`Browser → API Gateway → VPC Link → Internal ALB → EKS Services`
(§17). API Gateway owns edge/API concerns (throttling, Cognito
authorizer); the internal ALB routes to EKS services. A Backend-for-
Frontend is **not** introduced — the spec is explicit that BFF should
only appear if there's a genuine client-composition problem (§17), and
none exists yet: the frontend talks directly to the three core service
APIs.

## Alternatives considered
- **ALB-only (public-facing), no API Gateway.** Rejected: API Gateway
  gives Cognito authorizer integration and throttling "for free" at the
  edge, which is the point of putting it there rather than
  re-implementing throttling/authorizer logic in every service.
- **Introducing a BFF now "to be safe."** Explicitly rejected per §17 —
  adding an aggregation layer before any client-composition problem
  exists would be exactly the kind of premature abstraction the project
  rules warn against (§39.6, Ponytail-style: don't build what isn't
  needed yet).

## Consequences
- Nothing is provisioned yet — Phase 10 Terraform work. Locally
  (Phase 0–9), the frontend talks directly to each service's port via
  Docker Compose networking; no gateway/ALB simulation is attempted
  locally since it would just be scaffolding around scaffolding.
- If a real client-composition problem shows up later (e.g. the
  dashboard needing data fan-out from all three services in one round
  trip), a BFF can be added then, informed by an actual need instead of
  a guess.
