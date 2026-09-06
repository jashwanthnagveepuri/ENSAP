import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listSites, type Site, type SiteStatus } from '../../api/siteProfile'

const STATUSES: SiteStatus[] = ['ACTIVE', 'PENDING', 'DECOMMISSIONED']
const PAGE_SIZE = 20

/** FR-1.2 search sites by name/region/status, paginated (docs/02-functional-requirements.md). */
export default function SiteSearch() {
  const [status, setStatus] = useState('')
  const [region, setRegion] = useState('')
  const [name, setName] = useState('')
  const [page, setPage] = useState(0)
  const [sites, setSites] = useState<Site[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    setError(null)
    listSites({ status: status || undefined, region: region || undefined, name: name || undefined, page, size: PAGE_SIZE })
      .then((result) => {
        setSites(result.content)
        setTotalElements(result.totalElements)
      })
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
      .finally(() => setLoading(false))
  }, [status, region, name, page])

  const lastPage = Math.max(0, Math.ceil(totalElements / PAGE_SIZE) - 1)

  return (
    <section>
      <h1>Site Search</h1>
      <form onSubmit={(e) => e.preventDefault()}>
        <label>
          Name{' '}
          <input value={name} onChange={(e) => { setName(e.target.value); setPage(0) }} />
        </label>{' '}
        <label>
          Region{' '}
          <input value={region} onChange={(e) => { setRegion(e.target.value); setPage(0) }} />
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
      {error && <p role="alert">Failed to load sites: {error}</p>}

      {!error && (
        <table>
          <thead>
            <tr><th>ID</th><th>Name</th><th>Region</th><th>Status</th><th>Last refreshed</th></tr>
          </thead>
          <tbody>
            {sites.map((site) => (
              <tr key={site.id}>
                <td><Link to={`/sites/${site.id}`}>{site.id}</Link></td>
                <td>{site.name}</td>
                <td>{site.region}</td>
                <td>{site.status}</td>
                <td>{site.lastRefreshedAt ?? 'never'}</td>
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
