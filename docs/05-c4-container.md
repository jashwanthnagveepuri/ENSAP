# 05 — C4 Container Diagram (Level 2)

Containers inside the ENSAP system boundary. Matches the "Final
Architectural Mental Model" in the master spec (§42) and the high-level
architecture in §4.1.

```mermaid
flowchart TB
    subgraph edge["Edge (AWS, later phase)"]
      route53["Route 53"]
      cloudfront["CloudFront"]
      apigw["API Gateway"]
    end

    frontend["Frontend<br/>React + TypeScript + Vite<br/>(operator console)"]

    subgraph core["Core services (Spring Boot 3 / WebFlux / functional endpoints)"]
      siteSvc["Site Profile Service"]
      deploySvc["Deployment Service"]
      evidenceSvc["Evidence/Audit Service"]
    end

    camunda["Camunda 8 (Zeebe)<br/>workflow orchestration"]
    postgres[("PostgreSQL<br/>system of record")]
    redis[("Redis<br/>cache / rate limiting")]
    kafka["Kafka<br/>async event transport"]
    s3[("S3<br/>evidence & artifacts")]

    subgraph workers["Provider Workers"]
      routerW["Router Worker"]
      switchW["Switch Worker"]
      wirelessW["Wireless Worker"]
      firewallW["Firewall Worker"]
      ticketW["Ticketing Worker"]
    end

    subgraph mocks["Mock Provider APIs"]
      mockRouter["Mock Router API"]
      mockSwitch["Mock Switch API"]
      mockWireless["Mock Wireless API"]
      mockFirewall["Mock Firewall API"]
      mockTicket["Mock Ticket API"]
      mockSource["Mock Location/Inventory/Network APIs"]
    end

    route53 --> cloudfront --> frontend
    frontend --> apigw --> siteSvc
    apigw --> deploySvc
    apigw --> evidenceSvc

    siteSvc --> postgres
    deploySvc --> postgres
    evidenceSvc --> postgres
    siteSvc --> redis
    siteSvc --> mockSource

    deploySvc -- "start/signal process" --> camunda
    camunda -- "job workers" --> routerW
    camunda --> switchW
    camunda --> wirelessW
    camunda --> firewallW
    camunda --> ticketW

    postgres -- "transactional outbox" --> kafka
    kafka --> routerW
    kafka --> switchW
    kafka --> wirelessW
    kafka --> firewallW
    kafka --> ticketW
    kafka -- "step result events" --> deploySvc
    kafka -- "audit/evidence events" --> evidenceSvc

    routerW --> mockRouter
    switchW --> mockSwitch
    wirelessW --> mockWireless
    firewallW --> mockFirewall
    ticketW --> mockTicket

    evidenceSvc --> s3
```

## Container responsibilities

| Container | Owns | Talks to |
|---|---|---|
| Frontend | UI only, no business rules | Core services via REST (through API Gateway in AWS) |
| Site Profile Service | site/device/network-profile/WAN/VLAN/subnet data | PostgreSQL, Redis, mock source systems |
| Deployment Service | deployment/batch/step lifecycle, idempotency keys | PostgreSQL, Camunda, outbox→Kafka |
| Evidence/Audit Service | evidence metadata, audit events | PostgreSQL, S3, Kafka (consumes result events) |
| Camunda 8 (Zeebe) | workflow instance state, retries, timers | Deployment Service (start/signal), job workers |
| Workers (5) | provider-specific execution | Kafka, mock provider APIs |
| PostgreSQL | system of record for all services (logical ownership per service, §13.2) | — |
| Redis | cache/rate-limit only, not durable state | Site Profile Service |
| Kafka | async event transport, not a system of record | producers/consumers above |
| S3 | evidence artifacts, later frontend assets | Evidence/Audit Service |

See `06-component-design.md` for the internal (Level 3) package layout
of each service.
