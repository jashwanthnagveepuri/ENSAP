import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import DeploymentHistory from '../../../src/pages/deployments/DeploymentHistory'
import { listDeployments } from '../../../src/api/deployment'

vi.mock('../../../src/api/deployment')

describe('DeploymentHistory', () => {
  it('lists deployments with a link to their progress page', async () => {
    vi.mocked(listDeployments).mockResolvedValue({
      content: [{ id: 'DEP-1', siteId: 'SITE-1', status: 'RUNNING', createdAt: 'now', updatedAt: 'now' }],
      page: 0,
      size: 20,
      totalElements: 1,
    })
    render(<DeploymentHistory />, { wrapper: MemoryRouter })
    expect(await screen.findByRole('link', { name: 'DEP-1' })).toHaveAttribute('href', '/deployments/DEP-1/progress')
  })

  it('surfaces API errors (e.g. the 501 stub before Phase 2 ships)', async () => {
    vi.mocked(listDeployments).mockRejectedValue(new Error('This endpoint is a Phase 0 scaffold stub'))
    render(<DeploymentHistory />, { wrapper: MemoryRouter })
    expect(await screen.findByRole('alert')).toHaveTextContent('Phase 0 scaffold stub')
  })
})
