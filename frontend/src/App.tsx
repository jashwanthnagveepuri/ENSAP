import type { ComponentType } from 'react'
import { BrowserRouter, NavLink, Routes, Route } from 'react-router-dom'
import PageStub from './pages/PageStub'
import Dashboard from './pages/Dashboard'
import SiteSearch from './pages/sites/SiteSearch'
import SiteDetails from './pages/sites/SiteDetails'
import RefreshSite from './pages/sites/RefreshSite'
import StartDeployment from './pages/deployments/StartDeployment'
import DeploymentProgress from './pages/deployments/DeploymentProgress'
import DeploymentHistory from './pages/deployments/DeploymentHistory'
import { routes } from './routes'

/** Routes with a real page component; everything else still renders PageStub. */
const pageComponents: Record<string, ComponentType> = {
  '/': Dashboard,
  '/sites': SiteSearch,
  '/sites/:siteId': SiteDetails,
  '/sites/:siteId/refresh': RefreshSite,
  '/deployments/start': StartDeployment,
  '/deployments/:deploymentId/progress': DeploymentProgress,
  '/deployments/history': DeploymentHistory,
}

/**
 * ENSAP operator console shell (master spec §5). The frontend never
 * enforces authorization by itself — the backend validates the JWT and
 * enforces RBAC on every mutating call (docs/12-security.md); this shell
 * only renders navigation and routes.
 */
export default function App() {
  return (
    <BrowserRouter>
      <header>
        <h1>ENSAP Operator Console</h1>
      </header>
      <nav>
        <ul>
          {routes.map((r) => (
            <li key={r.path}>
              <NavLink to={r.path.replace(/:[^/]+/g, 'demo')} end={r.path === '/'}>{r.title}</NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <main>
        <Routes>
          {routes.map((r) => {
            const Page = pageComponents[r.path]
            return (
              <Route
                key={r.path}
                path={r.path}
                element={Page ? <Page /> : <PageStub title={r.title} phase={r.phase} />}
              />
            )
          })}
        </Routes>
      </main>
    </BrowserRouter>
  )
}
