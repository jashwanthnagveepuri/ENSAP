import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import RefreshSite from '../../../src/pages/sites/RefreshSite'
import { refreshSite } from '../../../src/api/siteProfile'

vi.mock('../../../src/api/siteProfile')

function renderAt(siteId: string) {
  return render(
    <MemoryRouter initialEntries={[`/sites/${siteId}/refresh`]}>
      <Routes>
        <Route path="/sites/:siteId/refresh" element={<RefreshSite />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('RefreshSite', () => {
  it('shows warnings when a source system is unavailable', async () => {
    vi.mocked(refreshSite).mockResolvedValue({
      site: { id: 'SITE-1', name: 'Acme', region: 'us-east-1', status: 'ACTIVE', lastRefreshedAt: '2026-01-01T00:00:00Z', sourceSystem: 'location-service', devices: [], networkProfiles: [], wanCircuits: [] },
      warnings: ['inventory-service unavailable; devices not updated'],
    })

    renderAt('SITE-1')
    fireEvent.click(screen.getByRole('button', { name: 'Refresh now' }))

    expect(await screen.findByText(/inventory-service unavailable/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View site details' })).toHaveAttribute('href', '/sites/SITE-1')
  })

  it('shows an error if the refresh call fails', async () => {
    vi.mocked(refreshSite).mockRejectedValue(new Error('Site not found'))
    renderAt('SITE-1')
    fireEvent.click(screen.getByRole('button', { name: 'Refresh now' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Site not found')
  })
})
