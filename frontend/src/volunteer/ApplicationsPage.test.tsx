import { notifications } from '@mantine/notifications'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { json, myApplication, volunteerUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { ApplicationsPage } from './ApplicationsPage'

vi.mock('@mantine/notifications', () => ({ notifications: { show: vi.fn() } }))

const routes = [{ path: '/volunteer/applications', element: <ApplicationsPage /> }]
// Far in the future, so nothing has started by the browser's clock.
const upcoming = { activityStartsAt: '2099-11-07T09:00:00', activityEndsAt: '2099-11-07T12:00:00' }

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.mocked(notifications.show).mockReset()
})

describe('ApplicationsPage', () => {
  it('splits invitations from applications and offers the actions of each', async () => {
    stubFetch({
      'GET /volunteers/me/applications': () =>
        json([
          myApplication({ id: 1, origin: 'INVITATION', activityTitle: 'Invited one', ...upcoming }),
          myApplication({ id: 2, activityTitle: 'Applied one', ...upcoming }),
          myApplication({ id: 3, status: 'ACCEPTED', activityTitle: 'Accepted one', ...upcoming }),
        ]),
    })
    renderRoutes(routes, '/volunteer/applications', volunteerUser)

    const invitations = await screen.findByTestId('invitations')
    expect(within(invitations).getByText('Invited one')).toBeInTheDocument()
    expect(within(invitations).getByRole('button', { name: 'Accept' })).toBeEnabled()
    expect(within(invitations).getByRole('button', { name: 'Decline' })).toBeInTheDocument()
    expect(within(invitations).queryByRole('button', { name: 'Withdraw' })).toBeNull()

    const applications = screen.getByTestId('applications')
    expect(within(applications).getAllByRole('button', { name: 'Withdraw' })).toHaveLength(2)
    expect(within(applications).queryByRole('button', { name: 'Accept' })).toBeNull()
  })

  it('accepts an invitation with a POST and shows a 409 as a message', async () => {
    const fetchMock = stubFetch({
      'GET /volunteers/me/applications': () => json([myApplication({ id: 1, origin: 'INVITATION', ...upcoming })]),
      'POST /invitations/1/accept': () => json({ message: 'Activity 7 has no free spots' }, 409),
    })
    renderRoutes(routes, '/volunteer/applications', volunteerUser)

    await userEvent.click(await screen.findByRole('button', { name: 'Accept' }))

    await waitFor(() =>
      expect(notifications.show).toHaveBeenCalledWith(
        expect.objectContaining({ color: 'red', message: 'Activity 7 has no free spots' }),
      ),
    )
    expect(fetchMock.mock.calls.some(([url, init]) => String(url) === '/api/v1/invitations/1/accept' && init?.method === 'POST')).toBe(true)
  })

  it('withdraws an accepted application only after a confirmation that the spot is freed', async () => {
    let withdrawn = false
    const fetchMock = stubFetch({
      'GET /volunteers/me/applications': () =>
        json([myApplication({ id: 3, status: withdrawn ? 'WITHDRAWN' : 'ACCEPTED', ...upcoming })]),
      'POST /applications/3/withdraw': () => {
        withdrawn = true
        return json(myApplication({ id: 3, status: 'WITHDRAWN', ...upcoming }))
      },
    })
    renderRoutes(routes, '/volunteer/applications', volunteerUser)

    await userEvent.click(await screen.findByRole('button', { name: 'Withdraw' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/Your spot in "Sort food donations" is freed/)).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/withdraw'))).toBe(false)

    await userEvent.click(within(dialog).getByRole('button', { name: 'Withdraw' }))

    // A withdrawn row is no longer current.
    await waitFor(() => expect(screen.queryByTestId('own-application')).toBeNull())
    await userEvent.click(screen.getByText('Past'))
    expect(await screen.findByText('WITHDRAWN')).toBeInTheDocument()
  })

  it('declines an invitation after the confirmation', async () => {
    const fetchMock = stubFetch({
      'GET /volunteers/me/applications': () => json([myApplication({ id: 1, origin: 'INVITATION', ...upcoming })]),
      'POST /invitations/1/decline': () => json(myApplication({ id: 1, origin: 'INVITATION', status: 'DECLINED' })),
    })
    renderRoutes(routes, '/volunteer/applications', volunteerUser)

    await userEvent.click(await screen.findByRole('button', { name: 'Decline' }))
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Decline' }))

    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/invitations/1/decline')).toBe(true))
  })

  it('shows rows of cancelled activities under "Past" without actions', async () => {
    stubFetch({
      'GET /volunteers/me/applications': () =>
        json([myApplication({ status: 'ACCEPTED', activityStatus: 'CANCELLED', ...upcoming })]),
    })
    renderRoutes(routes, '/volunteer/applications', volunteerUser)

    await screen.findByText('No applications here.', { exact: false })
    await userEvent.click(screen.getByText('Past'))

    const row = await screen.findByTestId('own-application')
    expect(within(row).getByText('Activity cancelled')).toBeInTheDocument()
    expect(within(row).queryByRole('button')).toBeNull()
  })
})
