# router-provider (mock)

Served by [`mocks/provider-mock`](../provider-mock/README.md) at
`/api/v1/router/jobs`, called by `workers/router-worker`. One
configurable Spring WebFlux app serves all five provider mocks by path
instead of five near-duplicate services (master spec §22). Supports
controllable behavior: success, 400, 401, 404, 409, 429, 500, 503,
timeout, fixed/random latency, temporary outage, random failure
percentage, duplicate callback, rate limiting — configurable at runtime
via `provider-mock`'s `/admin/providers/router` endpoint, which backs the
frontend's Provider Simulation/Admin page.
