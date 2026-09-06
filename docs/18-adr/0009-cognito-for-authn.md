# ADR-0009: Cognito for authentication/authorization

## Status
Accepted (design-only; implemented in Phase 7)

## Context
The platform needs real user authentication with role-based access
(VIEWER/OPERATOR/ADMIN, §6) that works in AWS without the team running
its own identity provider, plus a local-dev mode that doesn't require
AWS credentials (§2.3).

## Decision
Use **AWS Cognito User Pool** (OAuth2/OIDC, JWT access tokens,
group-based RBAC claims) in AWS. Local development uses a JWT-compatible
stand-in issuing tokens with the same claim shape, so backend
authorization code (JWT validation + role check) is identical in both
environments — only the token issuer changes (§6).

## Alternatives considered
- **Roll a custom auth service (users table + JWT issuance).** Rejected:
  reimplements a solved problem (password storage, MFA, token rotation)
  for no learning value beyond what Cognito integration already teaches,
  and the spec names Cognito as the "final decision" (§6).
- **Auth0/Okta.** Reasonable alternatives; rejected only because Cognito
  is the AWS-native choice consistent with the rest of the stack
  (API Gateway, IAM) and is explicitly specified (§6).

## Consequences
- Backend authorization logic must be written against JWT
  claims/groups, not against a specific IdP SDK, so the local
  dev-mode/Cognito swap (§6) stays a configuration change.
- Nothing is implemented yet — Phase 0 endpoints are unauthenticated
  stubs (`12-security.md`).
