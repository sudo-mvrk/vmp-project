import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { OwnVolunteerProfile } from '../api/types'
import { activity, json, score, volunteerUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { RecommendationsPage } from './RecommendationsPage'

const profile = { lat: 50.08, lon: 14.42 } as OwnVolunteerProfile

function renderPage() {
  return renderRoutes([{ path: '/volunteer/recommendations', element: <RecommendationsPage /> }], '/volunteer/recommendations', volunteerUser)
}

afterEach(() => vi.unstubAllGlobals())

describe('RecommendationsPage', () => {
  it('keeps the order of the response, not of the totals', async () => {
    // A lower total first: the page must not sort on its own.
    stubFetch({
      'GET /volunteers/me': () => json(profile),
      'GET /volunteers/me/recommended-activities': () =>
        json([
          { activity: activity({ id: 1, title: 'First' }), score: score({ total: 0.3 }) },
          { activity: activity({ id: 2, title: 'Second' }), score: score({ total: 0.9 }) },
        ]),
    })
    renderPage()

    const rows = await screen.findAllByTestId('recommendation')
    expect(rows.map((row) => within(row).getByRole('link').textContent)).toEqual(['First', 'Second'])
    expect(within(rows[0]).getByTestId('score-total')).toHaveTextContent('0.30')
    expect(within(rows[0]).getByRole('button', { name: 'Apply' })).toBeInTheDocument()
  })

  it('asks for 10 by default', async () => {
    const fetchMock = stubFetch({
      'GET /volunteers/me': () => json(profile),
      'GET /volunteers/me/recommended-activities': () => json([]),
    })
    renderPage()

    await screen.findByText('No activity fits your profile right now.')
    expect(fetchMock.mock.calls.map(([url]) => String(url))).toContain('/api/v1/volunteers/me/recommended-activities?limit=10')
  })

  it('explains an empty list and a missing location', async () => {
    stubFetch({
      'GET /volunteers/me': () => json({ ...profile, lat: null, lon: null }),
      'GET /volunteers/me/recommended-activities': () => json([]),
    })
    renderPage()

    expect(await screen.findByText('No activity fits your profile right now.')).toBeInTheDocument()
    expect(await screen.findByText(/Your profile has no location/)).toBeInTheDocument()
  })
})
