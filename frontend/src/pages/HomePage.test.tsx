import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderRoutes } from '../test/render'
import { HomePage } from './HomePage'

const routes = [
  { path: '/', element: <HomePage /> },
  { path: '/admin/users', element: <p>admin users</p> },
  { path: '/volunteer/recommendations', element: <p>volunteer home</p> },
]

describe('HomePage', () => {
  it('describes the platform to an anonymous visitor', async () => {
    renderRoutes(routes, '/', null)

    expect(await screen.findByRole('heading', { name: 'The right volunteer for every task.' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Browse activities/ })).toHaveAttribute('href', '/activities')
    expect(screen.getAllByRole('link', { name: /Create an account|Join the platform/ })[0]).toHaveAttribute('href', '/register')
    expect(screen.getByLabelText('Example of a score breakdown')).toHaveTextContent('0.85')
  })

  it('sends a signed-in admin to the users page, no longer to an outdated note', async () => {
    renderRoutes(routes, '/', { id: 9, username: 'admin', email: 'a@example.org', roles: ['ROLE_ADMIN'] })

    expect(await screen.findByText('admin users')).toBeInTheDocument()
  })

  it('sends a signed-in volunteer to their recommendations', async () => {
    renderRoutes(routes, '/', { id: 1, username: 'vol', email: 'v@example.org', roles: ['ROLE_VOLUNTEER'] })

    expect(await screen.findByText('volunteer home')).toBeInTheDocument()
  })
})
