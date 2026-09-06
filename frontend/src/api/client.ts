/**
 * Placeholder API client. Phase 0: no backend calls are wired up yet —
 * each page adds its own fetch calls against the relevant service
 * (docs/10-api-design.md) as it's implemented. Centralized here only to
 * fix the base-URL/correlation-header convention ahead of time.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export async function apiFetch(path: string, init?: RequestInit): Promise<Response> {
  return fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })
}
