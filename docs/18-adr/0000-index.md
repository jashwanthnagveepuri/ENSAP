# Architecture Decision Records — Index

| # | Title |
|---|---|
| [0001](0001-coarse-grained-service-boundaries.md) | Coarse-grained service boundaries (why microservices, why not more of them) |
| [0002](0002-camunda-8-for-workflow-orchestration.md) | Camunda 8 for workflow orchestration |
| [0003](0003-kafka-for-async-messaging.md) | Kafka for async messaging, `siteId` as partition key |
| [0004](0004-transactional-outbox-pattern.md) | Transactional outbox pattern |
| [0005](0005-postgresql-as-system-of-record.md) | PostgreSQL (local) / Aurora (AWS) as system of record |
| [0006](0006-redis-for-cache-and-rate-limiting.md) | Redis for caching/rate limiting, not durable state |
| [0007](0007-s3-for-evidence-and-artifacts.md) | S3 for evidence & artifact storage |
| [0008](0008-eks-for-compute.md) | EKS for backend compute |
| [0009](0009-cognito-for-authn.md) | Cognito for authentication/authorization |
| [0010](0010-api-gateway-plus-alb.md) | API Gateway + internal ALB for the API layer |
| [0011](0011-spring-functional-endpoints.md) | Spring functional endpoints over `@RestController` |
| [0012](0012-ai-outside-critical-path.md) | Operations Copilot kept outside the critical provisioning path |

Format: lightweight [Michael Nygard style](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
(Status / Context / Decision / Alternatives considered / Consequences).
