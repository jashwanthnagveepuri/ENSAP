# ADR-0008: EKS for backend compute

## Status
Accepted (design-only; implemented in Phase 9/10)

## Context
Backend services and workers need independent scaling (a Router Worker
under provider rate limits scales differently than the Deployment
Service), rolling deploys, and self-healing (§16), and the project's
explicit learning goals include Kubernetes and EKS (§3).

## Decision
Target **Amazon EKS** for services and workers in AWS, with multiple
replicas across 3 AZs, readiness/liveness probes, resource
requests/limits, HPA, Pod Disruption Budgets, topology spreading, and
NetworkPolicies where appropriate (§16). Locally, the same containers
run via Docker Compose (Phase 0–8) and then validated on `kind`/
`minikube` (Phase 9) **before** touching EKS (§26) — cost and iteration
speed matter more than cloud-fidelity during core development.

## Alternatives considered
- **ECS/Fargate.** A reasonable, simpler AWS compute choice. Rejected
  because Kubernetes/EKS specifically is a named learning goal (§3) and
  the spec's recommended topology is explicitly EKS-based (§16, §42).
- **Going straight to EKS without a local Docker Compose environment
  first.** Rejected — §2.3 requires the core application to run locally
  without AWS credentials; EKS is deliberately the last compute step,
  not the first (§37 Phase 9 before Phase 10).

## Consequences
- No EKS work happens before Phase 9; Phase 0–8 develop and test
  entirely against Docker Compose, which must therefore be a faithful
  enough environment (same images, same config surface) that the EKS
  jump in Phase 9/10 is mechanical, not a rewrite.
- Kubernetes manifests/Helm charts (`infrastructure/kubernetes`,
  `infrastructure/helm`) stay empty placeholders until Phase 9.
