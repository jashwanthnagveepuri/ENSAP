/**
 * Client for deployment-service (docs/10-api-design.md,
 * services/deployment-service/src/main/resources/openapi/deployment-service.yaml).
 * Phase 0/2: the OpenAPI contract exists but handlers return 501 until
 * Phase 2 lands the behavior — built contract-first so it needs no changes
 * once that ships. Base URL defaults to the local docker-compose port
 * mapping; override with VITE_DEPLOYMENT_API_BASE_URL for other environments.
 */
import { apiFetch, type Page } from './client'

const BASE_URL = import.meta.env.VITE_DEPLOYMENT_API_BASE_URL ?? 'http://localhost:8082'

export type DeploymentStatus =
  | 'REQUESTED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'FAILED_REQUIRES_ATTENTION'
  | 'CANCELLED'

export const TERMINAL_STATUSES: DeploymentStatus[] = ['COMPLETED', 'FAILED', 'FAILED_REQUIRES_ATTENTION', 'CANCELLED']

export type DeploymentStepStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export interface DeploymentStep {
  id: string
  stepName: string
  status: DeploymentStepStatus
  attemptCount: number
  lastError: string | null
  startedAt: string | null
  completedAt: string | null
}

export interface Deployment {
  id: string
  siteId: string
  batchId?: string | null
  status: DeploymentStatus
  workflowInstanceId?: string | null
  requestedBy?: string | null
  createdAt: string
  updatedAt: string
  steps?: DeploymentStep[]
}

export interface DeploymentSearchParams {
  status?: string
  siteId?: string
  page?: number
  size?: number
}

export function listDeployments(params: DeploymentSearchParams = {}): Promise<Page<Deployment>> {
  const qs = new URLSearchParams()
  if (params.status) qs.set('status', params.status)
  if (params.siteId) qs.set('siteId', params.siteId)
  qs.set('page', String(params.page ?? 0))
  qs.set('size', String(params.size ?? 20))
  return apiFetch(BASE_URL, `/api/deployments?${qs}`)
}

export function getDeployment(deploymentId: string): Promise<Deployment> {
  return apiFetch(BASE_URL, `/api/deployments/${encodeURIComponent(deploymentId)}`)
}

/** POST /api/deployments requires an Idempotency-Key (docs/10-api-design.md §12). */
export function createDeployment(siteId: string, idempotencyKey: string): Promise<Deployment> {
  return apiFetch(BASE_URL, '/api/deployments', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ siteId }),
  })
}

export function retryDeployment(deploymentId: string): Promise<void> {
  return apiFetch(BASE_URL, `/api/deployments/${encodeURIComponent(deploymentId)}/retry`, { method: 'POST' })
}

export function cancelDeployment(deploymentId: string): Promise<void> {
  return apiFetch(BASE_URL, `/api/deployments/${encodeURIComponent(deploymentId)}/cancel`, { method: 'POST' })
}
