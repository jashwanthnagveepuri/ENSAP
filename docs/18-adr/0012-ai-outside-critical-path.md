# ADR-0012: Operations Copilot kept outside the critical provisioning path

## Status
Accepted (design-only; implemented in Phase 12, last)

## Context
Agentic AI/tool-calling is an explicit learning goal (§3, §30) but the
project's core purpose is demonstrating deterministic distributed-
systems engineering. If AI sits in the deployment path, a model
hallucination or provider outage could directly cause an incorrect
network change — unacceptable even in a simulated system, and it would
also mean the AI feature can't be built/evaluated until the fragile
core is done.

## Decision
The **Operations Copilot** is a read-only investigative assistant added
only after the deterministic platform is stable (Phase 12, last phase).
It gets read-only tools (`getSite`, `getDeployment`,
`getDeploymentEvents`, `getFailedSteps`, `searchRunbook`) plus one
mutating tool, `retryDeployment`, which **must require explicit human
approval** before executing (§30). No AI component sits on the path
from "operator clicks start deployment" to "provider API is called."

## Alternatives considered
- **AI-assisted retry/remediation suggestions built earlier, in
  parallel with core phases.** Rejected: the spec is explicit ("Do not
  put AI in the critical network-provisioning path", §30) and doing this
  work before the deterministic system is stable risks building the
  Copilot against APIs/behavior that are still changing.
- **Fully autonomous remediation (AI directly calls `retryDeployment`
  without approval).** Explicitly rejected — mutation requires a human
  in the loop, always (§30).

## Consequences
- Phases 1–11 must expose whatever read APIs the Copilot will eventually
  need (deployment status, failed steps, audit events) as ordinary
  operator-console APIs anyway — no AI-specific backend work is deferred
  by this decision, only the AI orchestration layer itself.
- Nothing AI-related exists in this repository yet; this ADR exists so
  the constraint is on record before any AI code is written.
