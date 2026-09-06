# 16 — Runbooks (grows with each phase)

Phase 0 has no running business logic, so there are no operational
incidents to run books for yet. This file is the home for
operator-facing "what do I do when X happens" procedures as each phase
adds real behavior — later consumed read-only by the Operations Copilot
(§30, Phase 12).

## Template for future entries

```text
## <Symptom / alert name>

**When you see:** <observable signal — log line, dashboard, alert>
**Likely cause(s):** <ranked list>
**Check:** <commands/dashboards to confirm>
**Fix:** <operator action, e.g. "POST /api/deployments/{id}/retry">
**Escalate if:** <condition>
```

## Planned first entries (added when their phase lands)

- Deployment stuck in `FAILED_REQUIRES_ATTENTION` (Phase 5).
- Kafka consumer lag growing on `ensap.router.events` (Phase 6/8).
- Mock provider simulation left in a failure-injection state after a
  demo (Phase 4 — Admin page also has a "reset to healthy" action).
- Outbox events accumulating unpublished (Phase 3 — outbox publisher
  health).
