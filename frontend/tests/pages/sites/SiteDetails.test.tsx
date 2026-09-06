import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import SiteDetails from '../../../src/pages/sites/SiteDetails'
import { getSite } from '../../../src/api/siteProfile'

vi.mock('../../../src/api/siteProfile')

function renderAt(siteId: string) {
  return render(
    <MemoryRouter initialEntries={[`/sites/${siteId}`]}>
      <Routes>
        <Route path="/sites/:siteId" element={<SiteDetails />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('SiteDetails', () => {
  it('renders site, devices, and network profile from the API', async () => {
    vi.mocked(getSite).mockResolvedValue({
      id: 'SITE-1',
      name: 'Acme HQ',
      region: 'us-east-1',
      status: 'ACTIVE',
      lastRefreshedAt: '2026-01-01T00:00:00Z',
      sourceSystem: 'manual',
      devices: [{ id: 'DEV-1', siteId: 'SITE-1', type: 'router', vendor: 'Cisco', model: 'ISR', serialNumber: 'SN1', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' }],
      networkProfiles: [{ id: 'NP-1', siteId: 'SITE-1', profileName: 'core', createdAt: '2026-01-01T00:00:00Z', vlans: [{ id: 'VLAN-1', vlanTag: 10, name: 'data' }], subnets: [{ id: 'SUB-1', cidr: '10.0.0.0/24', purpose: 'data' }] }],
      wanCircuits: [{ id: 'WAN-1', carrier: 'AT&T', circuitId: 'C1', bandwidthMbps: 100 }],
    })

    renderAt('SITE-1')

    expect(await screen.findByRole('heading', { name: 'Acme HQ (SITE-1)' })).toBeInTheDocument()
    expect(screen.getByText('Cisco')).toBeInTheDocument()
    expect(screen.getByText('VLAN 10: data')).toBeInTheDocument()
    expect(screen.getByText('Subnet 10.0.0.0/24 (data)')).toBeInTheDocument()
    expect(screen.getByText('AT&T')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Refresh this site' })).toHaveAttribute('href', '/sites/SITE-1/refresh')
  })

  it('surfaces a 404 as an error state', async () => {
    vi.mocked(getSite).mockRejectedValue(new Error('Site SITE-404 not found'))
    renderAt('SITE-404')
    expect(await screen.findByRole('alert')).toHaveTextContent('Site SITE-404 not found')
  })
})
