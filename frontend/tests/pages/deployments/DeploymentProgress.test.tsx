import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import DeploymentProgress from '../../../src/pages/deployments/DeploymentProgress'
import { getDeployment } from '../../../src/api/deployment'
import { listAuditEvents } from '../../../src/api/auditEvent'

vi.mock('../../../src/api/deployment', async () => {
  const actual = await vi.importActual<typeof import('../../../src/api/deployment')>('../../../src/api/deployment')
  return { ...actual, getDeployment: vi.fn() }
})
vi.mock('../../../src/api/auditEvent')

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
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'COMPLETED', createdAt: 'now', updatedAt: 'now' })
    vi.mocked(listAuditEvents).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0 })
    renderAt('DEP-1')
    expect(await screen.findByText(/Status: COMPLETED/)).toBeInTheDocument()
    expect(getDeployment).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument()
  })

  it('offers a Retry action when a deployment requires attention', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'FAILED_REQUIRES_ATTENTION', createdAt: 'now', updatedAt: 'now' })
    vi.mocked(listAuditEvents).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0 })
    renderAt('DEP-1')
    expect(await screen.findByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })

  it('offers a Cancel action while a deployment is still in progress', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'RUNNING', createdAt: 'now', updatedAt: 'now' })
    vi.mocked(listAuditEvents).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0 })
    renderAt('DEP-1')
    expect(await screen.findByRole('button', { name: 'Cancel' })).toBeInTheDocument()
  })

  it('shows the step timeline embedded on the deployment', async () => {
    vi.mocked(getDeployment).mockResolvedValue({
      id: 'DEP-1',
      siteId: 'SITE-1',
      status: 'RUNNING',
      createdAt: 'now',
      updatedAt: 'now',
      steps: [
        { id: 'STEP-1', stepName: 'PROVISION', status: 'COMPLETED', attemptCount: 1, lastError: null, startedAt: 't0', completedAt: 't1' },
        { id: 'STEP-2', stepName: 'CONFIGURE', status: 'FAILED', attemptCount: 2, lastError: 'timeout', startedAt: 't1', completedAt: null },
      ],
    })
    vi.mocked(listAuditEvents).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0 })
    renderAt('DEP-1')
    expect(await screen.findByText('PROVISION')).toBeInTheDocument()
    expect(screen.getByText('CONFIGURE')).toBeInTheDocument()
    expect(screen.getByText('timeout')).toBeInTheDocument()
  })

  it('shows related audit events for the deployment', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'RUNNING', createdAt: 'now', updatedAt: 'now' })
    vi.mocked(listAuditEvents).mockResolvedValue({
      content: [{ id: 'EVT-1', deploymentId: 'DEP-1', siteId: 'SITE-1', actor: 'operator', eventType: 'DEPLOYMENT_STARTED', createdAt: 'now' }],
      page: 0,
      size: 20,
      totalElements: 1,
    })
    renderAt('DEP-1')
    expect(await screen.findByText(/DEPLOYMENT_STARTED/)).toBeInTheDocument()
    expect(listAuditEvents).toHaveBeenCalledWith({ deploymentId: 'DEP-1' })
  })

  it('surfaces audit event load failures without blocking the rest of the page', async () => {
    vi.mocked(getDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'RUNNING', createdAt: 'now', updatedAt: 'now' })
    vi.mocked(listAuditEvents).mockRejectedValue(new Error('audit service unavailable'))
    renderAt('DEP-1')
    expect(await screen.findByText(/Failed to load audit events/)).toBeInTheDocument()
  })
})
