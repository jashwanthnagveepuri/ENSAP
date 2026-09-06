# ADR-0007: S3 for evidence & artifact storage

## Status
Accepted (Phase 0)

## Context
Deployment evidence (provider request/response payloads, validation
reports, deployment summaries) can be large, is written once and read
rarely, and doesn't need relational querying — storing it as `bytea`/
`jsonb` blobs in PostgreSQL would bloat the operational database for no
benefit (§15).

## Decision
Use **Amazon S3** for evidence/artifact bodies; PostgreSQL
(`deployment_evidence` table, owned by Evidence/Audit Service) stores
only metadata plus the S3 key. Enable versioning, encryption, and
lifecycle policies (later phase, when actually provisioned via
Terraform). CloudFront + Origin Access Control fronts the bucket used
for the built frontend so the bucket itself is never a public origin
(§15).

## Alternatives considered
- **Store evidence bodies directly in PostgreSQL.** Rejected: large
  binary/JSON blobs in the transactional database hurt backup size,
  vacuum performance, and connection-pool memory for data that's
  write-once/read-rarely — exactly the case object storage is designed
  for.
- **A local filesystem volume for evidence in Compose, S3 only in
  AWS.** Considered as a Phase-0-simpler option, but rejected in favor
  of using an S3-compatible target (e.g. a local MinIO container in
  Compose, added when evidence storage is actually implemented in
  Phase 2) from the start, so the same code path works locally and in
  AWS without a storage-backend branch.

## Consequences
- Evidence storage isn't implemented until Phase 2; this ADR fixes the
  target so `deployment_evidence.s3_key` is the schema shape from the
  first Liquibase changeset (`09-database-design.md`) instead of being
  redesigned later.
- Local Compose will need a local S3-compatible store when this lands;
  not yet added since nothing writes evidence today.
