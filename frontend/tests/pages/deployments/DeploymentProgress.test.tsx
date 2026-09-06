import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import DeploymentProgress from '../../../src/pages/deployments/DeploymentProgress'
import { getDeployment } from '../../../src/api/deployment'

vi.mock('../../../src/api/deployment', async () => {
  const actual = await vi.importActual<typeof import('../../../src/api/deployment')>('../../../src/api/deployment')
  return { ...actual, getDeployment: vi.fn() }
})

function renderAt(deploymentId: string) {
  return render(
    <MemoryRouter initialEntries={[`/deployments/${deploymentId}/progress`]}>
      <Routes>
        <Route path="/deployments/:deploymentId/progress" element={<DeploymentProgress />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('DeploymentProgress', () => {
  it('shows the current status and stops polling once terminal', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'SUCCEEDED', createdAt: 'now', updatedAt: 'now' })
    renderAt('DEP-1')
    expect(await screen.findByText(/Status: SUCCEEDED/)).toBeInTheDocument()
    expect(getDeployment).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument()
  })

  it('offers a Retry action when a deployment requires attention', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'FAILED_REQUIRES_ATTENTION', createdAt: 'now', updatedAt: 'now' })
    renderAt('DEP-1')
    expect(await screen.findByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })

  it('offers a Cancel action while a deployment is still in progress', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'IN_PROGRESS', createdAt: 'now', updatedAt: 'now' })
    renderAt('DEP-1')
    expect(await screen.findByRole('button', { name: 'Cancel' })).toBeInTheDocument()
  })
})
