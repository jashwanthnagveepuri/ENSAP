# router-provider (mock)

Placeholder (Phase 4). Mock router provisioning API called by
`workers/router-worker`. Supports controllable behavior: success, 400,
401, 404, 409, 429, 500, 503, timeout, fixed/random latency, temporary
outage, random failure percentage, duplicate callback, rate limiting
(master spec §22) — configurable at runtime from the frontend's
Provider Simulation/Admin page.
