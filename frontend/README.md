# frontend

ENSAP operator console (React + TypeScript + Vite, master spec §5).
Phase 0: route shell only — all 13 required pages
(`src/routes.ts`) render a `PageStub` placeholder; each is replaced by a
real page as its backing service API lands
(`../docs/02-functional-requirements.md`).

The frontend is never the sole enforcement point for authorization —
the backend validates the JWT and enforces RBAC on every mutating call
(`../docs/12-security.md`).

## Run locally

```bash
npm install
npm run dev
```

## Build & test

```bash
npm run build   # tsc + vite build
npm test        # vitest — one smoke test today
```
