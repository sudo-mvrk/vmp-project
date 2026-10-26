import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { json, myApplication, organizationUser, volunteerUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { AppLayout } from './AppLayout'

const routes = [{ element: <AppLayout />, children: [{ path: '/', element: <p>home</p> }] }]
const upcoming = { activityStartsAt: '2099-11-07T09:00:00', activityEndsAt: '2099-11-07T12:00:00' }

afterEach(() => vi.unstubAllGlobals())

describe('AppLayout', () => {
  it('counts pending invitations next to "My applications" for a volunteer', async () => {
    stubFetch({
      'GET /volunteers/me/applications': () =>
        json([
          myApplication({ id: 1, origin: 'INVITATION', ...upcoming }),
          myApplication({ id: 2, origin: 'INVITATION', ...upcoming }),
          myApplication({ id: 3, ...upcoming }),
        ]),
    })
    renderRoutes(routes, '/', volunteerUser)

    expect(await screen.findByLabelText('2 pending invitations')).toHaveTextContent('2')
  })

  it('asks nothing about applications for an organization', async () => {
    const fetchMock = stubFetch({})
    renderRoutes(routes, '/', organizationUser)

    expect(await screen.findByText('home')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
