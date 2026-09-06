import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import PageStub from './pages/PageStub'
import { routes } from './routes'

/**
 * ENSAP operator console shell (master spec §5). The frontend never
 * enforces authorization by itself — the backend validates the JWT and
 * enforces RBAC on every mutating call (docs/12-security.md); this shell
 * only renders navigation and routes.
 */
export default function App() {
  return (
    <BrowserRouter>
      <nav>
        <ul>
          {routes.map((r) => (
            <li key={r.path}>
              <Link to={r.path.replace(/:[^/]+/g, 'demo')}>{r.title}</Link>
            </li>
          ))}
        </ul>
      </nav>
      <main>
        <Routes>
          {routes.map((r) => (
            <Route key={r.path} path={r.path} element={<PageStub title={r.title} phase={r.phase} />} />
          ))}
        </Routes>
      </main>
    </BrowserRouter>
  )
}
