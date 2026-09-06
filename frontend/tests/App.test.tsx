import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from '../src/App'

vi.mock('../src/api/siteProfile', () => ({
  listSites: vi.fn().mockResolvedValue({ content: [], page: 0, size: 1, totalElements: 3 }),
}))

describe('App', () => {
  it('renders the Dashboard link and page title', async () => {
    render(<App />)
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    await screen.findByText('3 sites on file.')
  })

  it('still stubs pages not yet built (e.g. Batch Deployment)', () => {
    render(<App />)
    fireEvent.click(screen.getByRole('link', { name: 'Batch Deployment' }))
    expect(screen.getByText(/Not implemented yet/)).toBeInTheDocument()
  })
})
