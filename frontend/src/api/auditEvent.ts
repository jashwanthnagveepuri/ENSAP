/**
 * Client for evidence-audit-service's audit endpoint (docs/10-api-design.md,
 * services/evidence-audit-service/src/main/resources/openapi/evidence-audit-service.yaml).
 * Phase 2/3: contract-first — handlers return 501 until the backend's
 * Kafka/outbox eventing + audit consumer lands. Base URL defaults to the
 * local docker-compose port mapping; override with
 * VITE_EVIDENCE_AUDIT_API_BASE_URL for other environments.
 */
import { apiFetch, type Page } from './client'

const BASE_URL = import.meta.env.VITE_EVIDENCE_AUDIT_API_BASE_URL ?? 'http://localhost:8083'

export interface AuditEvent {
  id: string
  deploymentId: string
  siteId: string
  actor: string
  eventType: string
  createdAt: string
}

export interface AuditEventSearchParams {
  deploymentId?: string
  siteId?: string
  page?: number
  size?: number
}

export function listAuditEvents(params: AuditEventSearchParams = {}): Promise<Page<AuditEvent>> {
  const qs = new URLSearchParams()
  if (params.deploymentId) qs.set('deploymentId', params.deploymentId)
  if (params.siteId) qs.set('siteId', params.siteId)
  qs.set('page', String(params.page ?? 0))
  qs.set('size', String(params.size ?? 20))
  return apiFetch(BASE_URL, `/api/audit-events?${qs}`)
}
