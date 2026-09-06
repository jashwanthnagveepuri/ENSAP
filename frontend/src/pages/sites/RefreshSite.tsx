import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { refreshSite, type SiteDetail } from '../../api/siteProfile'

/** FR-1.4: refresh a site profile from mock source systems, surfacing per-source warnings. */
export default function RefreshSite() {
  const { siteId } = useParams<{ siteId: string }>()
  const [status, setStatus] = useState<'idle' | 'loading' | 'done' | 'error'>('idle')
  const [site, setSite] = useState<SiteDetail | null>(null)
  const [warnings, setWarnings] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)

  function handleRefresh() {
    if (!siteId) return
    setStatus('loading')
    setError(null)
    refreshSite(siteId)
      .then((result) => {
        setSite(result.site)
        setWarnings(result.warnings)
        setStatus('done')
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : String(err))
        setStatus('error')
      })
  }

  return (
    <section>
      <h1>Refresh Site {siteId}</h1>
      <p>Pulls the latest data from the mock location/inventory/network source systems (§22) and replaces this site's devices and network profile.</p>
      <button onClick={handleRefresh} disabled={status === 'loading'}>
        {status === 'loading' ? 'Refreshing…' : 'Refresh now'}
      </button>

      {status === 'error' && <p role="alert">Refresh failed: {error}</p>}

      {status === 'done' && site && (
        <>
          <p>Refreshed at {site.lastRefreshedAt}.</p>
          {warnings.length > 0 && (
            <ul role="alert">
              {warnings.map((w) => <li key={w}>{w}</li>)}
            </ul>
          )}
          <p><Link to={`/sites/${site.id}`}>View site details</Link></p>
        </>
      )}
    </section>
  )
}
