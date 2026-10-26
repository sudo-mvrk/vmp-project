import { notifications } from '@mantine/notifications'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminUser } from '../api/types'
import { json } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { UsersPage } from './UsersPage'

vi.mock('@mantine/notifications', () => ({ notifications: { show: vi.fn() } }))

const admin = { id: 9, username: 'admin', email: 'admin@example.org', roles: ['ROLE_ADMIN'] }
const routes = [{ path: '/admin/users', element: <UsersPage /> }]

const user = (overrides: Partial<AdminUser>): AdminUser => ({
  id: 1,
  username: 'seed_vol01',
  email: 'seed_vol01@example.com',
  roles: ['ROLE_VOLUNTEER'],
  banned: false,
  createdAt: '2026-10-25T10:00:00',
  ...overrides,
})
const page = (content: AdminUser[]) => json({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.mocked(notifications.show).mockReset()
})

const requested = (fetchMock: ReturnType<typeof stubFetch>) =>
  fetchMock.mock.calls.map(([url]) => String(url)).filter((url) => url.startsWith('/api/v1/admin/users?'))

describe('UsersPage', () => {
  it('shows ban, unban or nothing by role and status', async () => {
    stubFetch({
      'GET /admin/users': () =>
        page([
          user({ id: 1, username: 'active_vol' }),
          user({ id: 2, username: 'banned_org', roles: ['ROLE_ORGANIZATION'], banned: true }),
          user({ id: 9, username: 'admin', roles: ['ROLE_ADMIN'] }),
        ]),
    })
    renderRoutes(routes, '/admin/users', admin)

    const [vol, org, adm] = await screen.findAllByTestId('user-row')
    expect(within(vol).getByRole('button', { name: 'Ban' })).toBeInTheDocument()
    expect(within(org).getByRole('button', { name: 'Unban' })).toBeInTheDocument()
    expect(within(org).getByText('Banned')).toBeInTheDocument()
    expect(within(adm).queryByRole('button')).toBeNull()
  })

  it('turns the search and the filters into the request', async () => {
    const fetchMock = stubFetch({ 'GET /admin/users': () => page([]) })
    renderRoutes(routes, '/admin/users', admin)

    await screen.findByText('0 users')
    await userEvent.type(screen.getByLabelText('Search'), 'vol0')
    await userEvent.click(screen.getByRole('combobox', { name: 'Status' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Banned', hidden: true }))

    await waitFor(() =>
      expect(requested(fetchMock)).toContain('/api/v1/admin/users?query=vol0&banned=true&page=0&size=20'),
    )
    // The search waits until typing stops, so not every keystroke is a request.
    expect(requested(fetchMock).some((url) => url.includes('query=v&'))).toBe(false)
  })

  it('bans only after a confirmation that names the consequences', async () => {
    const fetchMock = stubFetch({
      'GET /admin/users': () => page([user({ id: 1 })]),
      'POST /admin/users/1/ban': () => json(user({ id: 1, banned: true })),
    })
    renderRoutes(routes, '/admin/users', admin)

    await userEvent.click(within((await screen.findAllByTestId('user-row'))[0]).getByRole('button', { name: 'Ban' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/withdrawn or declined/)).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/ban'))).toBe(false)

    await userEvent.click(within(dialog).getByRole('button', { name: 'Ban' }))

    await waitFor(() =>
      expect(notifications.show).toHaveBeenCalledWith(expect.objectContaining({ message: 'seed_vol01 is banned' })),
    )
  })

  it('unbans without asking and says nothing is restored; shows a refusal as a message', async () => {
    stubFetch({
      'GET /admin/users': () => page([user({ id: 1, banned: true })]),
      'POST /admin/users/1/unban': () => json({ message: 'Admins cannot be unbanned' }, 409),
    })
    renderRoutes(routes, '/admin/users', admin)

    await userEvent.click(await screen.findByRole('button', { name: 'Unban' }))

    await waitFor(() =>
      expect(notifications.show).toHaveBeenCalledWith(
        expect.objectContaining({ color: 'wine', message: 'Admins cannot be unbanned' }),
      ),
    )
  })
})
