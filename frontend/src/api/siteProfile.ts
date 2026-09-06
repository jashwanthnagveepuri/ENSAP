/**
 * Client for the LIVE Phase 1 site-profile-service (docs/10-api-design.md,
 * services/site-profile-service). Base URL defaults to the local
 * docker-compose port mapping (infrastructure/docker/docker-compose.yml);
 * override with VITE_SITE_PROFILE_API_BASE_URL for other environments.
 */
import { apiFetch, type Page } from './client'

const BASE_URL = import.meta.env.VITE_SITE_PROFILE_API_BASE_URL ?? 'http://localhost:8081'

export type SiteStatus = 'ACTIVE' | 'PENDING' | 'DECOMMISSIONED'

export interface Site {
  id: string
  name: string
  region: string
  status: SiteStatus
  lastRefreshedAt: string | null
}

export interface Device {
  id: string
  siteId: string
  type: string
  vendor: string
  model: string
  serialNumber: string
  status: string
  createdAt: string
}

export interface Vlan {
  id: string
  vlanTag: number
  name: string
}

export interface Subnet {
  id: string
  cidr: string
  purpose: string
}

export interface NetworkProfile {
  id: string
  siteId: string
  profileName: string
  createdAt: string
  vlans: Vlan[]
  subnets: Subnet[]
}

export interface WanCircuit {
  id: string
  carrier: string
  circuitId: string
  bandwidthMbps: number
}

export interface SiteDetail extends Site {
  sourceSystem: string | null
  devices: Device[]
  networkProfiles: NetworkProfile[]
  wanCircuits: WanCircuit[]
}

export interface RefreshResult {
  site: SiteDetail
  warnings: string[]
}

export interface SiteSearchParams {
  status?: string
  region?: string
  name?: string
  page?: number
  size?: number
}

export function listSites(params: SiteSearchParams = {}): Promise<Page<Site>> {
  const qs = new URLSearchParams()
  if (params.status) qs.set('status', params.status)
  if (params.region) qs.set('region', params.region)
  if (params.name) qs.set('name', params.name)
  qs.set('page', String(params.page ?? 0))
  qs.set('size', String(params.size ?? 20))
  return apiFetch(BASE_URL, `/api/sites?${qs}`)
}

export function getSite(siteId: string): Promise<SiteDetail> {
  return apiFetch(BASE_URL, `/api/sites/${encodeURIComponent(siteId)}`)
}

export function refreshSite(siteId: string): Promise<RefreshResult> {
  return apiFetch(BASE_URL, `/api/sites/${encodeURIComponent(siteId)}/refresh`, { method: 'POST' })
}
