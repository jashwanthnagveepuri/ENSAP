# provider-mock

One Spring WebFlux app standing in for all five infrastructure provider
APIs (master spec §22) — `router`, `switch`, `wireless`, `firewall`,
`ticketing` — via a path variable, instead of five near-duplicate
services. Backs `mocks/router-provider`, `mocks/switch-provider`,
`mocks/wireless-provider`, `mocks/firewall-provider` and
`mocks/ticketing-provider` (each of those READMEs points here).

## Job API (called by `workers/*-worker`)

```
POST /api/v1/{providerType}/jobs
```

`providerType` is one of `router|switch|wireless|firewall|ticketing`
(case-insensitive). Body is arbitrary JSON; include `"callbackUrl"` to
receive the duplicate-callback behavior (below). Response is either a
`200` `JobResponse` or a non-2xx `ApiError`, per the configured behavior.

## Admin / Provider-Simulation API

Backs the frontend Admin page for changing a provider's behavior live
during a demo.

```
GET   /admin/providers                       # every provider's current config
GET   /admin/providers/{providerType}
PUT   /admin/providers/{providerType}         # replace the config, body = ProviderBehavior
POST  /admin/providers/{providerType}/reset   # back to defaults (SUCCESS, no latency, etc.)
```

`ProviderBehavior` fields (master spec §22 — every knob is independent
and layers on top of the others):

| field | meaning |
|---|---|
| `responseMode` | `SUCCESS`\|`BAD_REQUEST`\|`UNAUTHORIZED`\|`NOT_FOUND`\|`CONFLICT`\|`RATE_LIMITED`\|`SERVER_ERROR`\|`SERVICE_UNAVAILABLE`\|`TIMEOUT` |
| `fixedLatencyMs` | flat delay added before responding |
| `randomLatencyMinMs`/`randomLatencyMaxMs` | additional random delay in this range |
| `outageEnabled` | when true, every request fails `503` regardless of `responseMode` (simulates the whole provider being down) |
| `randomFailurePercent` | 0-100, independent chance any request fails `500` |
| `duplicateCallback` | when true and the request body has `callbackUrl`, a successful job POSTs its result to that URL twice |
| `rateLimitPerMinute` | 0 = unlimited; above this, requests get `429` (rolling 60s window) |

`responseMode: TIMEOUT` hangs the response for ~65s (long enough that any
real client-side timeout, master spec §21, fires first) rather than
returning immediately.

## Run

```
./mvnw spring-boot:run
```

Health: `GET /actuator/health`.
