import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { activity, json, myApplication, organizationUser, volunteerUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { ActivityPage } from './ActivityPage'

vi.mock('react-leaflet', () => import('../test/leafletMock'))

const routes = [{ path: '/activities/:id', element: <ActivityPage /> }]
const organization = { id: 3, organizationName: 'Prague Food Bank' }

afterEach(() => vi.unstubAllGlobals())

describe('ActivityPage', () => {
  it('shows the activity, its organization, free spots and place to anyone', async () => {
    stubFetch({
      'GET /activities/7': () => json(activity()),
      'GET /organizations/3': () => json(organization),
    })
    renderRoutes(routes, '/activities/7', null)

    expect(await screen.findByRole('heading', { name: 'Sort food donations' })).toBeInTheDocument()
    expect(await screen.findByText('Prague Food Bank')).toBeInTheDocument()
    expect(screen.getByText('3 of 4 spots free')).toBeInTheDocument()
    expect(screen.getByTestId('marker')).toHaveAttribute('data-center', '50.08,14.42')
    expect(screen.getByRole('link', { name: 'Log in to apply' })).toBeInTheDocument()
  })

  it('shows a volunteer the status of their application instead of the button', async () => {
    stubFetch({
      'GET /activities/7': () => json(activity()),
      'GET /organizations/3': () => json(organization),
      'GET /volunteers/me/applications': () => json([myApplication({ status: 'ACCEPTED' })]),
    })
    renderRoutes(routes, '/activities/7', volunteerUser)

    expect(await screen.findByText('Application accepted')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Apply' })).not.toBeInTheDocument()
  })

  it('offers no apply to an organization', async () => {
    stubFetch({
      'GET /activities/7': () => json(activity()),
      'GET /organizations/3': () => json(organization),
    })
    renderRoutes(routes, '/activities/7', organizationUser)

    await screen.findByText('Prague Food Bank')
    expect(screen.queryByRole('button', { name: 'Apply' })).not.toBeInTheDocument()
    expect(screen.queryByText('Log in to apply')).not.toBeInTheDocument()
  })

  it('shows "not found" for a 404, for example a draft of another organization', async () => {
    stubFetch({ 'GET /activities/7': () => json({ message: 'Activity 7 not found' }, 404) })
    renderRoutes(routes, '/activities/7', volunteerUser)

    expect(await screen.findByText('Page not found')).toBeInTheDocument()
  })
})
