import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getSite, type SiteDetail } from '../../api/siteProfile'

/** FR-1.3: site + devices + network profile (VLANs/subnets) + WAN circuits. */
export default function SiteDetails() {
  const { siteId } = useParams<{ siteId: string }>()
  const [site, setSite] = useState<SiteDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!siteId) return
    getSite(siteId)
      .then(setSite)
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
  }, [siteId])

  if (error) return <section><h1>Site {siteId}</h1><p role="alert">Failed to load site: {error}</p></section>
  if (!site) return <section><h1>Site {siteId}</h1><p>Loading…</p></section>

  return (
    <section>
      <h1>{site.name} ({site.id})</h1>
      <p>
        Region: {site.region} · Status: {site.status} · Last refreshed: {site.lastRefreshedAt ?? 'never'}
        {site.sourceSystem ? ` · Source: ${site.sourceSystem}` : ''}
      </p>
      <p><Link to={`/sites/${site.id}/refresh`}>Refresh this site</Link></p>

      <h2>Devices</h2>
      {site.devices.length === 0 ? <p>No devices.</p> : (
        <table>
          <thead><tr><th>Type</th><th>Vendor</th><th>Model</th><th>Serial</th><th>Status</th></tr></thead>
          <tbody>
            {site.devices.map((d) => (
              <tr key={d.id}><td>{d.type}</td><td>{d.vendor}</td><td>{d.model}</td><td>{d.serialNumber}</td><td>{d.status}</td></tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Network profiles</h2>
      {site.networkProfiles.length === 0 ? <p>No network profiles.</p> : (
        <ul>
          {site.networkProfiles.map((np) => (
            <li key={np.id}>
              {np.profileName}
              <ul>
                {np.vlans.map((v) => <li key={v.id}>VLAN {v.vlanTag}: {v.name}</li>)}
                {np.subnets.map((s) => <li key={s.id}>Subnet {s.cidr} ({s.purpose})</li>)}
              </ul>
            </li>
          ))}
        </ul>
      )}

      <h2>WAN circuits</h2>
      {site.wanCircuits.length === 0 ? <p>No WAN circuits.</p> : (
        <table>
          <thead><tr><th>Carrier</th><th>Circuit ID</th><th>Bandwidth (Mbps)</th></tr></thead>
          <tbody>
            {site.wanCircuits.map((c) => (
              <tr key={c.id}><td>{c.carrier}</td><td>{c.circuitId}</td><td>{c.bandwidthMbps}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
