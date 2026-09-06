import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import StartDeployment from '../../../src/pages/deployments/StartDeployment'
import { createDeployment } from '../../../src/api/deployment'

vi.mock('../../../src/api/deployment')

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/deployments/start']}>
      <Routes>
        <Route path="/deployments/start" element={<StartDeployment />} />
        <Route path="/deployments/:deploymentId/progress" element={<div>Progress page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('StartDeployment', () => {
  it('navigates to the progress page once the deployment is created', async () => {
    vi.mocked(createDeployment).mockResolvedValue({ id: 'DEP-1', siteId: 'SITE-1', status: 'PENDING', createdAt: 'now', updatedAt: 'now' })

    renderPage()
    fireEvent.change(screen.getByLabelText('Site ID'), { target: { value: 'SITE-1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Start deployment' }))

    expect(await screen.findByText('Progress page')).toBeInTheDocument()
    expect(createDeployment).toHaveBeenCalledWith('SITE-1', expect.any(String))
  })

  it('shows an error if the create call fails', async () => {
    vi.mocked(createDeployment).mockRejectedValue(new Error('site not found'))
    renderPage()
    fireEvent.change(screen.getByLabelText('Site ID'), { target: { value: 'SITE-404' } })
    fireEvent.click(screen.getByRole('button', { name: 'Start deployment' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('site not found')
  })
})
