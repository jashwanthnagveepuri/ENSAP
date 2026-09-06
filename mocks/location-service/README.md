# location-service (mock)

Phase 1. Synthetic source-of-truth for site location/existence data that
`site-profile-service` reconciles against on refresh (master spec §22).

`GET /sites/{siteId}` returns `{siteId, name, region, status}` deterministically
synthesized from `siteId`. Controllable behavior via `?mode=`:

| mode | behavior |
|---|---|
| `success` (default) | 200 with synthetic data |
| `error` | 500 |
| `notfound` | 404 |
| `timeout` | 200 after a 5s delay (exceeds the caller's 3s timeout) |

Run: `./mvnw spring-boot:run` (port 8080; mapped to `8091` in
`infrastructure/docker/docker-compose.yml`).
