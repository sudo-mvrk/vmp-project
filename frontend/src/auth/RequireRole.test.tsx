import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { RouteObject } from 'react-router'
import { renderRoutes } from '../test/render'
import { RequireRole } from './RequireRole'

const routes: RouteObject[] = [
  { path: '/login', element: <p>login page</p> },
  { path: '/no-access', element: <p>no access page</p> },
  {
    element: <RequireRole role="VOLUNTEER" />,
    children: [{ path: '/volunteer/profile', element: <p>volunteer page</p> }],
  },
]

const volunteer = { id: 1, username: 'vol', email: 'vol@example.org', roles: ['ROLE_VOLUNTEER'] }
const organization = { id: 2, username: 'org', email: 'org@example.org', roles: ['ROLE_ORGANIZATION'] }

describe('RequireRole', () => {
  it('shows the page to the role it guards', async () => {
    renderRoutes(routes, '/volunteer/profile', volunteer)

    expect(await screen.findByText('volunteer page')).toBeInTheDocument()
  })

  it('sends an anonymous user to the login page and remembers where they wanted to go', async () => {
    const { router } = renderRoutes(routes, '/volunteer/profile?tab=skills', null)

    expect(await screen.findByText('login page')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(new URLSearchParams(router.state.location.search).get('next')).toBe('/volunteer/profile?tab=skills')
  })

  it('sends another role to the no access page', async () => {
    const { router } = renderRoutes(routes, '/volunteer/profile', organization)

    expect(await screen.findByText('no access page')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/no-access')
  })
})
