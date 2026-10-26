import { notifications } from '@mantine/notifications'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { activity, activityApplication, json, volunteerProfile } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderWithProviders } from '../test/render'
import { ApplicationsTable } from './ApplicationsTable'

vi.mock('@mantine/notifications', () => ({ notifications: { show: vi.fn() } }))

const now = new Date('2026-11-07T08:00:00')

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.mocked(notifications.show).mockReset()
})

function rows() {
  return screen.findAllByTestId('application-row')
}

describe('ApplicationsTable', () => {
  it('offers accept and reject only for pending applications, and waits on pending invitations', async () => {
    stubFetch({
      'GET /activities/7/applications': () =>
        json([
          activityApplication({ id: 1, volunteer: volunteerProfile({ firstName: 'Ann' }) }),
          activityApplication({ id: 2, origin: 'INVITATION', volunteer: volunteerProfile({ firstName: 'Ben' }) }),
          activityApplication({
            id: 3,
            status: 'ACCEPTED',
            volunteerEmail: 'cat@example.org',
            volunteer: volunteerProfile({ firstName: 'Cat' }),
          }),
        ]),
    })
    renderWithProviders(<ApplicationsTable activity={activity()} now={now} />)

    const [ann, ben, cat] = await rows()
    expect(within(ann).getByRole('button', { name: 'Accept' })).toBeEnabled()
    expect(within(ann).getByRole('button', { name: 'Reject' })).toBeInTheDocument()
    expect(within(ben).getByText('Waiting for the volunteer')).toBeInTheDocument()
    expect(within(ben).queryByRole('button', { name: /Accept|Reject/ })).toBeNull()
    expect(within(cat).queryByRole('button', { name: 'Accept' })).toBeNull()
    // Q7d: the email only for the accepted row.
    expect(within(cat).getByText('cat@example.org')).toBeInTheDocument()
    expect(within(ann).queryByText(/@/)).toBeNull()
  })

  it('disables accept when the activity is full', async () => {
    stubFetch({ 'GET /activities/7/applications': () => json([activityApplication()]) })
    renderWithProviders(<ApplicationsTable activity={activity({ spotsNeeded: 1, acceptedCount: 1 })} now={now} />)

    const [row] = await rows()
    expect(within(row).getByRole('button', { name: 'Accept' })).toBeDisabled()
  })

  it('accepts with a POST and shows a named 409 as a message', async () => {
    const fetchMock = stubFetch({
      'GET /activities/7/applications': () => json([activityApplication()]),
      'POST /applications/31/accept': () => json({ message: 'The volunteer has no free capacity (1 of 1 in use)' }, 409),
    })
    renderWithProviders(<ApplicationsTable activity={activity()} now={now} />)

    await userEvent.click(within((await rows())[0]).getByRole('button', { name: 'Accept' }))

    await waitFor(() =>
      expect(notifications.show).toHaveBeenCalledWith(
        expect.objectContaining({ color: 'wine', message: 'The volunteer has no free capacity (1 of 1 in use)' }),
      ),
    )
    expect(fetchMock.mock.calls.some(([url, init]) => String(url) === '/api/v1/applications/31/accept' && init?.method === 'POST')).toBe(true)
  })

  it('rejects only after the confirmation', async () => {
    const fetchMock = stubFetch({
      'GET /activities/7/applications': () => json([activityApplication()]),
      'POST /applications/31/reject': () => json(activityApplication({ status: 'REJECTED' })),
    })
    renderWithProviders(<ApplicationsTable activity={activity()} now={now} />)

    await userEvent.click(within((await rows())[0]).getByRole('button', { name: 'Reject' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/cannot apply to this activity again/)).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/reject'))).toBe(false)

    await userEvent.click(within(dialog).getByRole('button', { name: 'Reject' }))

    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/reject'))).toBe(true))
  })

  it('opens the volunteer profile without the date of birth', async () => {
    stubFetch({
      'GET /activities/7/applications': () =>
        json([
          activityApplication({
            volunteer: volunteerProfile({
              bio: 'I like sorting things',
              availability: [{ dayOfWeek: 'SATURDAY', start: '08:00:00', end: '14:00:00' }],
            }),
          }),
        ]),
    })
    renderWithProviders(<ApplicationsTable activity={activity()} now={now} />)

    await userEvent.click(await screen.findByRole('button', { name: 'Lucie Svoboda' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText('I like sorting things')).toBeInTheDocument()
    expect(within(dialog).getByText('Sat 08:00–14:00')).toBeInTheDocument()
    expect(within(dialog).queryByText(/1988/)).toBeNull()
  })
})
