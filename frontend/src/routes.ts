/**
 * The 13 operator console pages required by master spec §5. Phase 0:
 * every route renders PageStub; each is swapped for a real component as
 * its backing service API lands (docs/02-functional-requirements.md).
 */
export interface RouteDef {
  path: string
  title: string
  phase: string
}

export const routes: RouteDef[] = [
  { path: '/login', title: 'Login', phase: 'Phase 7 (Cognito / local JWT dev-mode)' },
  { path: '/', title: 'Dashboard', phase: 'Phase 1' },
  { path: '/sites', title: 'Site Search', phase: 'Phase 1' },
  { path: '/sites/:siteId', title: 'Site Details', phase: 'Phase 1' },
  { path: '/sites/:siteId/refresh', title: 'Refresh Site Profile', phase: 'Phase 1' },
  { path: '/deployments/start', title: 'Start Deployment', phase: 'Phase 2' },
  { path: '/deployments/batch', title: 'Batch Deployment', phase: 'Phase 6' },
  { path: '/deployments/:deploymentId/progress', title: 'Deployment Progress', phase: 'Phase 2' },
  { path: '/deployments/history', title: 'Deployment History', phase: 'Phase 2' },
  { path: '/deployments/retry-queue', title: 'Failure/Retry Queue', phase: 'Phase 3' },
  { path: '/deployments/:deploymentId/evidence', title: 'Evidence Viewer', phase: 'Phase 2' },
  { path: '/admin/provider-simulation', title: 'Provider Simulation/Admin', phase: 'Phase 4' },
  { path: '/audit', title: 'Audit History', phase: 'Phase 3' },
]
