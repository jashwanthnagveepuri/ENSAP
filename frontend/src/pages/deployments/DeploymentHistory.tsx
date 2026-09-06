import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listDeployments, type Deployment, type DeploymentStatus } from '../../api/deployment'

const STATUSES: DeploymentStatus[] = ['PENDING', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED', 'FAILED_REQUIRES_ATTENTION', 'CANCELLED']
const PAGE_SIZE = 20

export default function DeploymentHistory() {
  const [status, setStatus] = useState('')
  const [siteId, setSiteId] = useState('')
  const [page, setPage] = useState(0)
  const [deployments, setDeployments] = useState<Deployment[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    setError(null)
    listDeployments({ status: status || undefined, siteId: siteId || undefined, page, size: PAGE_SIZE })
      .then((result) => {
        setDeployments(result.content)
        setTotalElements(result.totalElements)
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setLoading(false))
  }, [status, siteId, page])

  const lastPage = Math.max(0, Math.ceil(totalElements / PAGE_SIZE) - 1)

  return (
    <section>
      <h1>Deployment History</h1>
      <form onSubmit={(e) => e.preventDefault()}>
        <label>
          Site ID{' '}
          <input value={siteId} onChange={(e) => { setSiteId(e.target.value); setPage(0) }} />
        </label>{' '}
        <label>
          Status{' '}
          <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
            <option value="">All</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
      </form>

      {loading && <p>Loading…</p>}
      {error && <p role="alert">Failed to load deployments: {error}</p>}

      {!error && (
        <table>
          <thead><tr><th>ID</th><th>Site</th><th>Status</th><th>Created</th><th>Updated</th></tr></thead>
          <tbody>
            {deployments.map((d) => (
              <tr key={d.id}>
                <td><Link to={`/deployments/${d.id}/progress`}>{d.id}</Link></td>
                <td>{d.siteId}</td>
                <td>{d.status}</td>
                <td>{d.createdAt}</td>
                <td>{d.updatedAt}</td>
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
