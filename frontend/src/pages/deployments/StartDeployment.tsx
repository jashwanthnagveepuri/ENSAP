import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createDeployment } from '../../api/deployment'

/**
 * FR-2.1 start a single-site deployment. Contract-first against
 * deployment-service's OpenAPI (currently 501 until Phase 2 lands) — an
 * Idempotency-Key is required on every POST (docs/10-api-design.md §12).
 */
export default function StartDeployment() {
  const [siteId, setSiteId] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    createDeployment(siteId, crypto.randomUUID())
      .then((deployment) => navigate(`/deployments/${deployment.id}/progress`))
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setSubmitting(false))
  }

  return (
    <section>
      <h1>Start Deployment</h1>
      <form onSubmit={handleSubmit}>
        <label>
          Site ID{' '}
          <input value={siteId} onChange={(e) => setSiteId(e.target.value)} required />
        </label>{' '}
        <button type="submit" disabled={submitting || !siteId}>
          {submitting ? 'Starting…' : 'Start deployment'}
        </button>
      </form>
      {error && <p role="alert">Failed to start deployment: {error}</p>}
    </section>
  )
}
