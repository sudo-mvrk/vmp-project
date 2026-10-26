import { notifications } from '@mantine/notifications'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { json, myApplication, volunteerUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { ApplyButton } from './ApplyButton'
import type { ApplyState } from './apply'

vi.mock('@mantine/notifications', () => ({ notifications: { show: vi.fn() } }))

function renderButton(state: ApplyState) {
  return renderRoutes([{ path: '/activities/7', element: <ApplyButton activityId={7} state={state} /> }], '/activities/7', volunteerUser)
}

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.mocked(notifications.show).mockReset()
})

describe('ApplyButton', () => {
  it('applies with a POST and refreshes the recommendations and own applications', async () => {
    const fetchMock = stubFetch({ 'POST /activities/7/applications': () => json(myApplication(), 201) })
    const { queryClient } = renderButton({ kind: 'apply' })
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')

    await userEvent.click(screen.getByRole('button', { name: 'Apply' }))

    await waitFor(() => expect(notifications.show).toHaveBeenCalledWith(expect.objectContaining({ color: 'green' })))
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0][1]?.headers).toMatchObject({ 'X-XSRF-TOKEN': 't' })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['volunteer', 'recommended-activities'] })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['volunteer', 'applications'] })
  })

  it('shows the message of a 409 from the backend', async () => {
    stubFetch({
      'POST /activities/7/applications': () => json({ message: 'You already applied to activity 7' }, 409),
    })
    renderButton({ kind: 'apply' })

    await userEvent.click(screen.getByRole('button', { name: 'Apply' }))

    await waitFor(() =>
      expect(notifications.show).toHaveBeenCalledWith(
        expect.objectContaining({ color: 'red', message: 'You already applied to activity 7' }),
      ),
    )
  })

  it('shows the status instead of a button once applied', () => {
    renderButton({ kind: 'applied', status: 'PENDING' })

    expect(screen.getByText('Application pending')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('sends an anonymous user to the login page and back', () => {
    renderButton({ kind: 'login' })

    expect(screen.getByRole('link', { name: 'Log in to apply' })).toHaveAttribute('href', '/login?next=%2Factivities%2F7')
  })

  it('disables applying to a full activity', () => {
    renderButton({ kind: 'full' })

    expect(screen.getByRole('button', { name: 'No free spots' })).toBeDisabled()
  })
})
