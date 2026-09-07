import { useEffect, useState } from 'react'
import { listAuditEvents, type AuditEvent } from '../../api/auditEvent'

const PAGE_SIZE = 20

/**
 * FR-4 audit trail (master spec §5). Lists audit events recorded by
 * evidence-audit-service's Kafka-consumer pipeline — contract-first
 * against docs/10-api-design.md + docs/11-event-catalog.md, best-effort
 * while that consumer is still being wired up on the backend.
 */
export default function AuditHistory() {
  const [siteId, setSiteId] = useState('')
  const [deploymentId, setDeploymentId] = useState('')
  const [page, setPage] = useState(0)
  const [events, setEvents] = useState<AuditEvent[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    setError(null)
    listAuditEvents({ siteId: siteId || undefined, deploymentId: deploymentId || undefined, page, size: PAGE_SIZE })
      .then((result) => {
        setEvents(result.content)
        setTotalElements(result.totalElements)
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setLoading(false))
  }, [siteId, deploymentId, page])

  const lastPage = Math.max(0, Math.ceil(totalElements / PAGE_SIZE) - 1)

  return (
    <section>
      <h1>Audit History</h1>
      <form onSubmit={(e) => e.preventDefault()}>
        <label>
          Site ID{' '}
          <input value={siteId} onChange={(e) => { setSiteId(e.target.value); setPage(0) }} />
        </label>{' '}
        <label>
          Deployment ID{' '}
          <input value={deploymentId} onChange={(e) => { setDeploymentId(e.target.value); setPage(0) }} />
        </label>
      </form>

      {loading && <p>Loading…</p>}
      {error && <p role="alert">Failed to load audit events: {error}</p>}

      {!error && (
        <table>
          <thead><tr><th>ID</th><th>Deployment</th><th>Site</th><th>Actor</th><th>Event type</th><th>Created</th></tr></thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.id}>
                <td>{event.id}</td>
                <td>{event.deploymentId}</td>
                <td>{event.siteId}</td>
                <td>{event.actor}</td>
                <td>{event.eventType}</td>
                <td>{event.createdAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <p>
        <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>{' '}
        Page {page + 1} of {lastPage + 1}{' '}
        <button disabled={page >= lastPage} onClick={() => setPage((p) => p + 1)}>Next</button>
      </p>
    </section>
  )
}
