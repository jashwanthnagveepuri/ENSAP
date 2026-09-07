import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import AuditHistory from '../../../src/pages/audit/AuditHistory'
import { listAuditEvents } from '../../../src/api/auditEvent'

vi.mock('../../../src/api/auditEvent')

describe('AuditHistory', () => {
  it('lists audit events', async () => {
    vi.mocked(listAuditEvents).mockResolvedValue({
      content: [{ id: 'EVT-1', deploymentId: 'DEP-1', siteId: 'SITE-1', actor: 'operator', eventType: 'DEPLOYMENT_STARTED', createdAt: 'now' }],
      page: 0,
      size: 20,
      totalElements: 1,
    })
    render(<AuditHistory />)
    expect(await screen.findByText('DEPLOYMENT_STARTED')).toBeInTheDocument()
    expect(screen.getByText('DEP-1')).toBeInTheDocument()
  })

  it('surfaces API errors (e.g. the 501 stub before the consumer ships)', async () => {
    vi.mocked(listAuditEvents).mockRejectedValue(new Error('This endpoint is a Phase 0 scaffold stub'))
    render(<AuditHistory />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Phase 0 scaffold stub')
  })
})
