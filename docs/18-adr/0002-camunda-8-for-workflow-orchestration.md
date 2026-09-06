# ADR-0002: Camunda 8 for workflow orchestration

## Status
Accepted (Phase 0)

## Context
A deployment is a multi-step, multi-service, long-running process that
must survive crashes, support retries/timeouts, and allow manual
intervention (§8). This state machine could be hand-rolled (a `status`
column + application code deciding "what's next"), or delegated to a
purpose-built durable workflow engine.

## Decision
Use **Camunda 8 (Zeebe)** as the workflow orchestrator. The
`deployment-process` BPMN diagram (`workflow/camunda/
deployment-process.bpmn`, described in `08-workflow.md`) is the single
place that encodes step order, retry policy, and manual-intervention
points. Camunda contains zero vendor-specific provisioning code —
workers do the actual work; Camunda only decides what happens next.

## Alternatives considered
- **Hand-rolled state machine in the Deployment Service** (a `status`
  enum + a scheduler polling for the next step). Rejected: reinvents
  durable timers, retry/backoff bookkeeping, and crash-recovery that a
  workflow engine already solves correctly — and defeats the explicit
  learning goal of demonstrating distributed workflow orchestration
  (§3).
- **Kafka-only choreography** (each worker reacts to the previous
  worker's completion event, no central coordinator). Rejected for the
  deployment lifecycle specifically: with 8 sequential steps and
  required manual-intervention points, an explicit sequence is easier to
  reason about and audit than events chained through five independent
  consumers (§34 — orchestration vs. choreography). Choreography is
  still used for decoupled side effects (e.g. Evidence/Audit Service
  reacting to step-completed events).
- **Temporal / AWS Step Functions.** Reasonable alternatives, not chosen
  because Camunda 8 (BPMN, open-source, learning-project-friendly, self-
  hosted locally via Docker) is the technology named in the spec's
  learning goals (§3) and is a widely-recognized enterprise workflow
  standard (BPMN 2.0).

## Consequences
- Adds an operational dependency (Zeebe broker + Operate) to the local
  Docker Compose stack (§25).
- Workers must be written as Zeebe job workers (or Kafka consumers
  signaling Camunda), an additional integration pattern to learn beyond
  plain Kafka consumers.
- Workflow history/audit is visible in Camunda Operate in addition to
  the `audit_event` table — two places show "what happened", which is
  intentional (workflow-engine view vs. business-audit view) but must be
  documented so it doesn't read as duplication by accident.
