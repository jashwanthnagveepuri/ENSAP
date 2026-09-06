import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  cancelDeployment,
  getDeployment,
  retryDeployment,
  TERMINAL_STATUSES,
  type Deployment,
} from '../../api/deployment'

const POLL_MS = 3000

/**
 * FR-2.2 track deployment status. Polls GET /api/deployments/{id} until a
 * terminal status is reached — deployment-service has no push/SSE channel
 * yet (docs/10-api-design.md), so polling is the simplest thing that works.
 */
export default function DeploymentProgress() {
  const { deploymentId } = useParams<{ deploymentId: string }>()
  const [deployment, setDeployment] = useState<Deployment | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

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
        </>
      )}
    </section>
  )
}
