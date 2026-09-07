import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listDeployments, retryDeployment, type Deployment } from '../../api/deployment'

const PAGE_SIZE = 20

/**
 * FR-2.3 failure/retry queue (master spec §5). Lists deployments stuck at
 * FAILED_REQUIRES_ATTENTION with a one-click retry (POST
 * /api/deployments/{id}/retry, already live in deployment-service).
 */
export default function RetryQueue() {
  const [page, setPage] = useState(0)
  const [deployments, setDeployments] = useState<Deployment[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [retryError, setRetryError] = useState<string | null>(null)
  const [retryingId, setRetryingId] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    listDeployments({ status: 'FAILED_REQUIRES_ATTENTION', page, size: PAGE_SIZE })
      .then((result) => {
        setDeployments(result.content)
        setTotalElements(result.totalElements)
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  function handleRetry(deploymentId: string) {
    setRetryError(null)
    setRetryingId(deploymentId)
    retryDeployment(deploymentId)
      .then(load)
      .catch((err) => setRetryError(err instanceof Error ? err.message : String(err)))
      .finally(() => setRetryingId(null))
  }

  const lastPage = Math.max(0, Math.ceil(totalElements / PAGE_SIZE) - 1)

  return (
    <section>
      <h1>Failure / Retry Queue</h1>
      {loading && <p>Loading…</p>}
      {error && <p role="alert">Failed to load retry queue: {error}</p>}
      {retryError && <p role="alert">Retry failed: {retryError}</p>}

      {!error && (
        <table>
          <thead><tr><th>ID</th><th>Site</th><th>Updated</th><th></th></tr></thead>
          <tbody>
            {deployments.map((d) => (
              <tr key={d.id}>
                <td><Link to={`/deployments/${d.id}/progress`}>{d.id}</Link></td>
                <td>{d.siteId}</td>
                <td>{d.updatedAt}</td>
                <td>
                  <button disabled={retryingId === d.id} onClick={() => handleRetry(d.id)}>
                    {retryingId === d.id ? 'Retrying…' : 'Retry'}
                  </button>
                </td>
              </tr>
            ))}
            {!loading && deployments.length === 0 && (
              <tr><td colSpan={4}>No deployments awaiting attention.</td></tr>
            )}
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
