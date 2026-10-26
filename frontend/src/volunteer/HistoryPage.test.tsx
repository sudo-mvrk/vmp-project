import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { json, volunteerUser } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderRoutes } from '../test/render'
import { HistoryPage } from './HistoryPage'

const routes = [{ path: '/volunteer/history', element: <HistoryPage /> }]

afterEach(() => vi.unstubAllGlobals())

const item = (applicationId: number, title: string) => ({
  applicationId,
  activityId: applicationId + 100,
  title,
  city: 'Prague',
  startsAt: '2026-10-25T09:00:00',
  endsAt: '2026-10-25T12:00:00',
  organizationId: 1,
  organizationName: 'Prague Food Bank',
})

describe('HistoryPage', () => {
  it('lists completed activities in the order of the backend with a count', async () => {
    stubFetch({ 'GET /volunteers/me/history': () => json([item(2, 'Newer'), item(1, 'Older')]) })
    renderRoutes(routes, '/volunteer/history', volunteerUser)

    const items = await screen.findAllByTestId('history-item')
    expect(items.map((i) => i.querySelector('a')?.textContent)).toEqual(['Newer', 'Older'])
    expect(items[0].querySelector('a')).toHaveAttribute('href', '/activities/102')
    expect(screen.getByTestId('history-count')).toHaveTextContent('2 activities completed')
    expect(screen.getAllByText('Prague Food Bank · Prague')).toHaveLength(2)
  })

  it('explains an empty history', async () => {
    stubFetch({ 'GET /volunteers/me/history': () => json([]) })
    renderRoutes(routes, '/volunteer/history', volunteerUser)

    expect(await screen.findByText(/once the organization marks them as completed/)).toBeInTheDocument()
    expect(screen.getByTestId('history-count')).toHaveTextContent('0 activities completed')
  })
})
