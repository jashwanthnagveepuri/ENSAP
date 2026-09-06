import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import SiteSearch from '../../../src/pages/sites/SiteSearch'
import { listSites } from '../../../src/api/siteProfile'

vi.mock('../../../src/api/siteProfile')

describe('SiteSearch', () => {
  it('lists sites returned by the API with a link to details', async () => {
    vi.mocked(listSites).mockResolvedValue({
      content: [{ id: 'SITE-1', name: 'Acme HQ', region: 'us-east-1', status: 'ACTIVE', lastRefreshedAt: null }],
      page: 0,
      size: 20,
      totalElements: 1,
    })
    render(<SiteSearch />, { wrapper: MemoryRouter })
    expect(await screen.findByRole('link', { name: 'SITE-1' })).toHaveAttribute('href', '/sites/SITE-1')
    expect(screen.getByText('Acme HQ')).toBeInTheDocument()
  })

  it('surfaces API errors', async () => {
    vi.mocked(listSites).mockRejectedValue(new Error('service down'))
    render(<SiteSearch />, { wrapper: MemoryRouter })
    expect(await screen.findByRole('alert')).toHaveTextContent('service down')
  })
})
