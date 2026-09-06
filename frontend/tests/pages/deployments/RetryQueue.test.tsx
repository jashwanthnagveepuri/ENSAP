import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import RetryQueue from '../../../src/pages/deployments/RetryQueue'
import { listDeployments, retryDeployment } from '../../../src/api/deployment'

vi.mock('../../../src/api/deployment')

describe('RetryQueue', () => {
  it('lists deployments requiring attention and retries one on click', async () => {
    vi.mocked(listDeployments).mockResolvedValue({
      content: [{ id: 'DEP-1', siteId: 'SITE-1', status: 'FAILED_REQUIRES_ATTENTION', createdAt: 'now', updatedAt: 'now' }],
      page: 0,
      size: 20,
      totalElements: 1,
    })
    vi.mocked(retryDeployment).mockResolvedValue(undefined)

    render(<RetryQueue />, { wrapper: MemoryRouter })
    expect(await screen.findByRole('link', { name: 'DEP-1' })).toHaveAttribute('href', '/deployments/DEP-1/progress')
    expect(listDeployments).toHaveBeenCalledWith({ status: 'FAILED_REQUIRES_ATTENTION', page: 0, size: 20 })

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(retryDeployment).toHaveBeenCalledWith('DEP-1')
    await screen.findByRole('link', { name: 'DEP-1' }) // let the post-retry reload settle before the test ends
  })

  it('shows an empty state when nothing needs attention', async () => {
    vi.mocked(listDeployments).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0 })
    render(<RetryQueue />, { wrapper: MemoryRouter })
    expect(await screen.findByText('No deployments awaiting attention.')).toBeInTheDocument()
  })

  it('surfaces retry failures', async () => {
    vi.mocked(listDeployments).mockResolvedValue({
      content: [{ id: 'DEP-1', siteId: 'SITE-1', status: 'FAILED_REQUIRES_ATTENTION', createdAt: 'now', updatedAt: 'now' }],
      page: 0,
      size: 20,
      totalElements: 1,
    })
    vi.mocked(retryDeployment).mockRejectedValue(new Error('deployment is not retryable'))

    render(<RetryQueue />, { wrapper: MemoryRouter })
    fireEvent.click(await screen.findByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('deployment is not retryable')
  })
})
