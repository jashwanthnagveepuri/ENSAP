# 11 — Event Catalog

Contracts documented with AsyncAPI at
`shared/event-contracts/asyncapi.yaml`; the envelope JSON Schema is at
`shared/event-contracts/schemas/event-envelope.schema.json`. Nothing
publishes/consumes these yet (Phase 3+) — Phase 0 only fixes the shape.

## Envelope (every event)

```json
{
  "eventId": "uuid",
  "eventType": "router.provisioning.requested",
  "eventVersion": 1,
  "occurredAt": "timestamp",
  "correlationId": "uuid",
  "traceId": "trace-id",
  "siteId": "SITE-001",
  "deploymentId": "DEP-001",
  "payload": {}
}
```

## Events

| Event | Producer | Consumer(s) | Topic |
|---|---|---|---|
| `site.profile.created` | Site Profile Service | Evidence/Audit Service | `ensap.site.events` |
| `site.profile.refreshed` | Site Profile Service | Evidence/Audit Service | `ensap.site.events` |
| `deployment.requested` | Deployment Service | Evidence/Audit Service | `ensap.deployment.events` |
| `deployment.started` | Deployment Service | Evidence/Audit Service | `ensap.deployment.events` |
| `deployment.validation.completed` | Deployment Service (worker via Camunda) | Deployment Service | `ensap.deployment.events` |
| `deployment.validation.failed` | Deployment Service | Deployment Service, Evidence/Audit Service | `ensap.deployment.events` |
| `router.provisioning.requested` | Deployment Service / Camunda | Router Worker | `ensap.router.events` |
| `router.provisioning.completed` | Router Worker | Deployment Service, Evidence/Audit Service | `ensap.router.events` |
| `router.provisioning.failed` | Router Worker | Deployment Service, Evidence/Audit Service | `ensap.router.events` |
| `switch.provisioning.requested` | Deployment Service / Camunda | Switch Worker | `ensap.switch.events` |
| `switch.provisioning.completed` | Switch Worker | Deployment Service, Evidence/Audit Service | `ensap.switch.events` |
| `wireless.provisioning.requested` | Deployment Service / Camunda | Wireless Worker | `ensap.wireless.events` |
| `wireless.provisioning.completed` | Wireless Worker | Deployment Service, Evidence/Audit Service | `ensap.wireless.events` |
| `firewall.provisioning.requested` | Deployment Service / Camunda | Firewall Worker | `ensap.firewall.events` |
| `firewall.provisioning.completed` | Firewall Worker | Deployment Service, Evidence/Audit Service | `ensap.firewall.events` |
| `deployment.completed` | Deployment Service | Evidence/Audit Service | `ensap.deployment.events` |
| `deployment.failed` | Deployment Service | Evidence/Audit Service | `ensap.deployment.events` |
| `deployment.cancelled` | Deployment Service | Evidence/Audit Service | `ensap.deployment.events` |
| `deployment.retry.requested` | Deployment Service | Deployment Service (Camunda signal) | `ensap.deployment.events` |

Per-provider `*.provisioning.failed` events (switch/wireless/firewall)
follow the same shape as `router.provisioning.failed` and are omitted
above for brevity — see the AsyncAPI document for the exhaustive list.

## Kafka rules applied (§9)

- **Partition key = `siteId`** where per-site ordering matters (all
  deployment/provisioning topics) — see
  `18-adr/0003-kafka-for-async-messaging.md`.
- Versioned schemas (`eventVersion`); breaking changes bump the version,
  never mutate a shipped shape in place.
- Consumer groups per logical consumer (one per service/worker type),
  offsets tracked by Kafka/consumer group, not by application state.
- Duplicate delivery is expected — every consumer must be idempotent
  (`processed_operation`, `operationId` convention, §12).
- Kafka is transport, not the system of record — durable state lives in
  PostgreSQL (outbox) and Camunda (workflow), never only in Kafka.

## Topics (local, single-broker KRaft)

`ensap.site.events`, `ensap.deployment.events`, `ensap.router.events`,
`ensap.switch.events`, `ensap.wireless.events`, `ensap.firewall.events`,
`ensap.ticketing.events` — each with a default of 6 partitions locally
so `siteId`-based partitioning is meaningfully exercised even at small
scale.
