# inventory-service (mock)

Phase 1. Synthetic source-of-truth for device inventory that
`site-profile-service` reconciles against on refresh — same
controllable-behavior pattern as `mocks/location-service`.

`GET /sites/{siteId}` returns `{siteId, devices: [{type, vendor, model,
serialNumber, status}, ...]}` deterministically synthesized from `siteId`.
Same `?mode=success|error|notfound|timeout` as location-service.

Run: `./mvnw spring-boot:run` (port 8080; mapped to `8092` in
`infrastructure/docker/docker-compose.yml`).
