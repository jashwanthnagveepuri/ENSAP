# 14 — Deployment (local Phase 0 → AWS Phase 10)

## Local (this phase)

`infrastructure/docker/docker-compose.yml` starts: PostgreSQL, Kafka
(KRaft, no Zookeeper), Redis, Camunda 8 (Zeebe broker + Operate), and
the three core Spring Boot services. Mock providers, workers, and the
frontend container are defined but commented out until their code
exists (Phase 1/4) — see the compose file header for exactly what's
live today.

```bash
cd infrastructure/docker
docker compose up -d postgres kafka redis zeebe operate
```

Each service can then be run individually with its Maven wrapper:

```bash
cd services/site-profile-service
./mvnw spring-boot:run
```

No AWS credentials are required to run any of this (§2.3).

## Kubernetes (Phase 9)

`infrastructure/kubernetes/` (raw manifests) and `infrastructure/helm/`
(charts) are placeholders today. Phase 9 will validate the same
workload on `kind`/`minikube` before EKS is ever touched (§26).

## AWS (Phase 10)

`infrastructure/terraform/` is a placeholder today. Phase 10 provisions
VPC, EKS, ECR, Aurora, MSK, Redis (ElastiCache), S3, CloudFront, Route
53, API Gateway, ALB, Cognito, IAM, KMS, Secrets Manager, Lambda,
EventBridge per module (§27).

## CI/CD (Phase 11)

`.github/workflows/ci.yml` currently runs compile + unit tests for the
Java services and build + test for the frontend on every push/PR — the
container build/scan/ECR-push/deploy-dev/smoke-test stages (§28) are
added in Phase 11.
