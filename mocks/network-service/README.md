# network-service (mock)

Phase 1. Synthetic source-of-truth for WAN circuit/network-profile data
that `site-profile-service` reconciles against on refresh — same
controllable-behavior pattern as `mocks/location-service`.

`GET /sites/{siteId}` returns `{siteId, networkProfiles: [{profileName,
vlans: [{vlanTag, name}], subnets: [{cidr, purpose}]}], wanCircuits:
[{carrier, circuitId, bandwidthMbps}]}` deterministically synthesized from
`siteId`. Same `?mode=success|error|notfound|timeout` as location-service.

Run: `./mvnw spring-boot:run` (port 8080; mapped to `8093` in
`infrastructure/docker/docker-compose.yml`).
