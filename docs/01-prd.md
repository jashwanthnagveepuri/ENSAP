# 01 — Product Requirements Document (PRD)

## Summary

ENSAP (Enterprise Network Site Automation Platform) is a personal learning
and portfolio project. It simulates the software platform an enterprise
network operations team would use to provision and track the rollout of
network equipment (routers, switches, wireless APs, firewalls) at a large
number of physical sites.

> This is a **learning implementation**, not a production system. All
> external providers (routers, switches, wireless, firewall, ticketing,
> location/inventory/circuit sources) are mocked. No real vendor APIs,
> credentials, or infrastructure IDs are used.

## Problem statement

Rolling out network equipment to thousands of enterprise sites is a
long-running, multi-step, multi-vendor process prone to partial failure:
a provider API times out, a device is temporarily unreachable, a step
needs manual retry. Operators need a system that tracks deployment state
reliably, survives crashes/restarts, and gives visibility into what
succeeded, what failed, and why — without ad-hoc spreadsheets or tribal
knowledge.

## Goals

- Model the full lifecycle of a site deployment: request → orchestrate →
  execute per-provider steps → collect evidence → complete/fail.
- Demonstrate durable workflow orchestration (Camunda 8) coordinating
  independently-scaling workers via asynchronous messaging (Kafka).
- Demonstrate resilience: retries, backoff, circuit breaking, idempotency,
  and manual recovery in the face of simulated provider failures.
- Provide an operator console (React) to search sites, start/track
  deployments, and inspect evidence/audit history.
- Provide a foundation that can later be deployed to AWS (EKS, Aurora,
  MSK, Cognito, etc.) without changing the core application architecture.

## Non-goals (for this phase and generally)

- Real integration with any vendor router/switch/wireless/firewall API.
- Production-grade multi-tenant security hardening.
- Being represented as an employer's production system.
- Building the complete system in one pass — the project is delivered
  phase-by-phase (see [`37-development-phases`](../README.md) in the
  master spec; phase status is tracked in the top-level README).

## Primary users / personas

- **Operator** — searches sites, starts/retries deployments, watches
  progress, reviews evidence.
- **Admin** — everything an Operator can do, plus manages provider
  simulation behavior (failure injection) for demos/testing.
- **Viewer** — read-only access to sites, deployments, and audit history.

## Success criteria (portfolio-level)

See `docs/17-testing-strategy.md` and §38 "Definition of Done" in the
master spec: documented architecture, working outbox + idempotent
consumers, durable Camunda workflow, simulated provider failures with
working retry/circuit-breaking, working RBAC, distributed tracing, local
Docker Compose environment, and (later phases) Kubernetes + Terraform +
CI/CD, with all benchmark numbers based on tests actually run.

## Phase 0 scope (this document set)

Architecture, documentation, and scaffolding only. No business logic is
implemented yet — see the top-level README for exactly what phase the
codebase is currently in.
