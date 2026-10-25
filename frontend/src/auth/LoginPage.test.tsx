import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderRoutes } from '../test/render'
import { LoginPage } from './LoginPage'

const fetchMock = vi.fn<typeof fetch>()

const routes = [
  { path: '/login', element: <LoginPage /> },
  { path: '/organization/activities', element: <p>organization home</p> },
  { path: '/volunteer/profile', element: <p>volunteer profile</p> },
]

function jsonResponse(status: number, body?: unknown) {
  return new Response(body === undefined ? null : JSON.stringify(body), { status })
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

async function logIn(username: string, password: string) {
  await userEvent.type(screen.getByLabelText('Username or email'), username)
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
}

describe('LoginPage', () => {
  it('shows the message of the backend when the login fails', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(401, { message: 'Invalid username or password' }))
    renderRoutes(routes, '/login', null)

    await logIn('seed_org1', 'wrong')

    expect(await screen.findByText('Invalid username or password')).toBeInTheDocument()
  })

  it('opens the home page of the role after a successful login', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200))
      .mockResolvedValueOnce(jsonResponse(200, { id: 1, username: 'seed_org1', roles: ['ROLE_ORGANIZATION'] }))
    renderRoutes(routes, '/login', null)

    await logIn('seed_org1', 'password1')

    expect(await screen.findByText('organization home')).toBeInTheDocument()
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/auth/me')
  })

  it('goes back to the page in ?next after login', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200))
      .mockResolvedValueOnce(jsonResponse(200, { id: 2, username: 'seed_vol08', roles: ['ROLE_VOLUNTEER'] }))
    renderRoutes(routes, '/login?next=%2Fvolunteer%2Fprofile', null)

    await logIn('seed_vol08', 'password1')

    expect(await screen.findByText('volunteer profile')).toBeInTheDocument()
  })

  it('ignores a ?next that leaves the app', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200))
      .mockResolvedValueOnce(jsonResponse(200, { id: 1, username: 'seed_org1', roles: ['ROLE_ORGANIZATION'] }))
    renderRoutes(routes, '/login?next=%2F%2Fevil.example', null)

    await logIn('seed_org1', 'password1')

    expect(await screen.findByText('organization home')).toBeInTheDocument()
  })
})
