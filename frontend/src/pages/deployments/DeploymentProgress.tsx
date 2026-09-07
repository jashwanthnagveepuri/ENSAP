import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  cancelDeployment,
  getDeployment,
  retryDeployment,
  TERMINAL_STATUSES,
  type Deployment,
} from '../../api/deployment'
import { listAuditEvents, type AuditEvent } from '../../api/auditEvent'

const POLL_MS = 3000

/**
 * FR-2.2 track deployment status. Polls GET /api/deployments/{id} until a
 * terminal status is reached — deployment-service has no push/SSE channel
 * yet (docs/10-api-design.md), so polling is the simplest thing that works.
 * Phase 3: also shows the step timeline (embedded on the same payload) and
 * the related audit events (evidence-audit-service, best-effort — its
 * eventing consumer may not be live yet).
 */
export default function DeploymentProgress() {
  const { deploymentId } = useParams<{ deploymentId: string }>()
  const [deployment, setDeployment] = useState<Deployment | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [events, setEvents] = useState<AuditEvent[]>([])
  const [eventsError, setEventsError] = useState<string | null>(null)

  useEffect(() => {
    if (!deploymentId) return
    let cancelled = false
    let timer: ReturnType<typeof setTimeout>

    async function poll() {
      try {
        const result = await getDeployment(deploymentId!)
        if (cancelled) return
        setDeployment(result)
        setError(null)
        if (!TERMINAL_STATUSES.includes(result.status)) {
          timer = setTimeout(poll, POLL_MS)
        }
      } catch (err) {
        if (cancelled) return
        setError(err instanceof Error ? err.message : String(err))
        timer = setTimeout(poll, POLL_MS)
      }
    }
    poll()

    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [deploymentId])

  useEffect(() => {
    if (!deploymentId) return
    listAuditEvents({ deploymentId })
      .then((result) => setEvents(result.content))
      .catch((err) => setEventsError(err instanceof Error ? err.message : String(err)))
  }, [deploymentId])

  function handleRetry() {
    if (!deploymentId) return
    setActionError(null)
    retryDeployment(deploymentId).catch((err) => setActionError(err instanceof Error ? err.message : String(err)))
  }

  function handleCancel() {
    if (!deploymentId) return
    setActionError(null)
    cancelDeployment(deploymentId).catch((err) => setActionError(err instanceof Error ? err.message : String(err)))
  }

  return (
    <section>
      <h1>Deployment {deploymentId}</h1>
      {error && <p role="alert">Failed to load status: {error} (retrying…)</p>}
      {!deployment && !error && <p>Loading…</p>}
      {deployment && (
        <>
          <p>
            Site: {deployment.siteId} · Status: {deployment.status} · Updated: {deployment.updatedAt}
          </p>
          {deployment.status === 'FAILED_REQUIRES_ATTENTION' && (
            <button onClick={handleRetry}>Retry</button>
          )}
          {!TERMINAL_STATUSES.includes(deployment.status) && (
            <button onClick={handleCancel}>Cancel</button>
          )}
          {actionError && <p role="alert">{actionError}</p>}

          <h2>Steps</h2>
          {deployment.steps && deployment.steps.length > 0 ? (
            <table>
              <thead>
                <tr><th>Step</th><th>Status</th><th>Attempts</th><th>Last error</th><th>Started</th><th>Completed</th></tr>
              </thead>
              <tbody>
                {deployment.steps.map((step) => (
                  <tr key={step.id}>
                    <td>{step.stepName}</td>
                    <td>{step.status}</td>
                    <td>{step.attemptCount}</td>
                    <td>{step.lastError ?? '—'}</td>
                    <td>{step.startedAt ?? '—'}</td>
                    <td>{step.completedAt ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p>No steps recorded yet.</p>
          )}

          <h2>Audit events</h2>
          {eventsError && <p role="alert">Failed to load audit events: {eventsError}</p>}
          {!eventsError && events.length === 0 && <p>No audit events recorded yet.</p>}
          {!eventsError && events.length > 0 && (
            <ul>
              {events.map((event) => (
                <li key={event.id}>
                  {event.createdAt} · {event.eventType} · {event.actor}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
