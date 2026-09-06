/**
 * Shared fetch helper for all service clients (docs/10-api-design.md error
 * model + pagination envelope). Each service (site-profile, deployment) has
 * its own base URL and its own typed client in this directory; this file
 * only fixes the request/error/pagination conventions common to all of them.
 */
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public correlationId?: string,
  ) {
    super(message)
  }
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}

export async function apiFetch<T>(baseUrl: string, path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })
  const text = await res.text()
  const body = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    throw new ApiError(res.status, body?.code ?? 'UNKNOWN', body?.message ?? res.statusText, body?.correlationId)
  }
  return body as T
}
