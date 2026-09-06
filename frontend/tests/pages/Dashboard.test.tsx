import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import Dashboard from '../../src/pages/Dashboard'
import { listSites } from '../../src/api/siteProfile'

vi.mock('../../src/api/siteProfile')

describe('Dashboard', () => {
  it('shows the live site count once the site-profile-service call resolves', async () => {
    vi.mocked(listSites).mockResolvedValue({ content: [], page: 0, size: 1, totalElements: 7 })
    render(<Dashboard />, { wrapper: MemoryRouter })
    expect(await screen.findByText('7 sites on file.')).toBeInTheDocument()
  })

  it('shows an error instead of crashing when the API call fails', async () => {
    vi.mocked(listSites).mockRejectedValue(new Error('boom'))
    render(<Dashboard />, { wrapper: MemoryRouter })
    expect(await screen.findByText(/Site count unavailable: boom/)).toBeInTheDocument()
  })
})
