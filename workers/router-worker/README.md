# router-worker

Placeholder (Phase 4). Will be a Zeebe job worker for the
`router-provision` job type (`workflow/camunda/deployment-process.bpmn`)
that calls `mocks/router-provider` with resilience policies (Phase 5)
and publishes `router.provisioning.completed`/`failed`
(`docs/11-event-catalog.md`). Owns no PostgreSQL tables — see
`docs/06-component-design.md`.
