import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listSites } from '../api/siteProfile'

/**
 * Console home (master spec §5). Just a jumping-off point to the real
 * work areas plus one live number (total sites) to prove the Phase 1 API
 * is actually wired up, not a static landing page.
 */
export default function Dashboard() {
  const [siteCount, setSiteCount] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listSites({ size: 1 })
      .then((page) => setSiteCount(page.totalElements))
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
  }, [])

  return (
    <section>
      <h1>Dashboard</h1>
      <p>
        {error ? `Site count unavailable: ${error}` : siteCount === null ? 'Loading site count…' : `${siteCount} sites on file.`}
      </p>
      <ul>
        <li><Link to="/sites">Search sites</Link></li>
        <li><Link to="/deployments/start">Start a deployment</Link></li>
        <li><Link to="/deployments/history">Deployment history</Link></li>
      </ul>
    </section>
  )
}
