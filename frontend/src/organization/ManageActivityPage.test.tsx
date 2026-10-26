import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { activity, json, organizationUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { ManageActivityPage } from './ManageActivityPage'

vi.mock('@mantine/notifications', () => ({ notifications: { show: vi.fn() } }))

const routes = [
  { path: '/organization/activities/:id', element: <ManageActivityPage /> },
  { path: '/activities/:id', element: <p>public page</p> },
]
const ownOrganization = { id: 3, username: 'org', organizationName: 'Prague Food Bank' }
// Far in the future, so an open activity has not started by the browser's clock.
const upcoming = { startsAt: '2099-11-07T09:00:00', endsAt: '2099-11-07T12:00:00' }

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => vi.unstubAllGlobals())

describe('ManageActivityPage', () => {
  it('offers edit, publish and cancel for a draft, and publishes without asking', async () => {
    const fetchMock = stubFetch({
      'GET /activities/7': () => json(activity({ status: 'DRAFT', ...upcoming })),
      'GET /organizations/me': () => json(ownOrganization),
      'POST /activities/7/publish': () => json(activity({ status: 'OPEN', ...upcoming })),
      'GET /organizations/me/activities': () => json([]),
    })
    renderRoutes(routes, '/organization/activities/7', organizationUser)

    expect(await screen.findByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/organization/activities/7/edit')
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Complete' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Publish' }))

    expect(await screen.findByText('OPEN')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url, init]) => String(url) === '/api/v1/activities/7/publish' && init?.method === 'POST')).toBe(true)
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument()
  })

  it('cancels only after the confirmation', async () => {
    const fetchMock = stubFetch({
      'GET /activities/7': () => json(activity({ status: 'OPEN', ...upcoming })),
      'GET /organizations/me': () => json(ownOrganization),
      'POST /activities/7/cancel': () => json(activity({ status: 'CANCELLED', ...upcoming })),
      'GET /organizations/me/activities': () => json([]),
      'GET /activities': () => json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    })
    renderRoutes(routes, '/organization/activities/7', organizationUser)

    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }))
    expect(await screen.findByText('Cancel this activity?')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/cancel'))).toBe(false)

    await userEvent.click(screen.getByRole('button', { name: 'Cancel activity' }))

    await waitFor(() => expect(screen.getByText('CANCELLED')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('shows the refusal of the backend as its message', async () => {
    stubFetch({
      'GET /activities/7': () => json(activity({ status: 'DRAFT', ...upcoming })),
      'GET /organizations/me': () => json(ownOrganization),
      'POST /activities/7/publish': () => json({ message: 'An activity must start in the future, got 2099-11-07T09:00' }, 400),
    })
    renderRoutes(routes, '/organization/activities/7', organizationUser)

    await userEvent.click(await screen.findByRole('button', { name: 'Publish' }))

    expect(await screen.findByText('An activity must start in the future, got 2099-11-07T09:00')).toBeInTheDocument()
  })

  it("sends another organization's activity to its public page", async () => {
    stubFetch({
      'GET /activities/7': () => json(activity({ organizationId: 99 })),
      'GET /organizations/me': () => json(ownOrganization),
    })
    renderRoutes(routes, '/organization/activities/7', organizationUser)

    expect(await screen.findByText('public page')).toBeInTheDocument()
  })
})
