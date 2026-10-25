import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { OwnVolunteerProfile } from '../api/types'
import { clickMap } from '../test/leafletMock'
import { renderWithProviders } from '../test/render'
import { BasicProfileForm } from './BasicProfileForm'

vi.mock('react-leaflet', () => import('../test/leafletMock'))

const fetchMock = vi.fn<typeof fetch>()

const profile: OwnVolunteerProfile = {
  username: 'vol',
  id: 1,
  firstName: 'Lucie',
  lastName: 'Svoboda',
  dateOfBirth: '1988-11-13',
  bio: null,
  lat: null,
  lon: null,
  locationName: null,
  countryCode: null,
  city: null,
  region: null,
  radius: null,
  maxActiveAssignments: 1,
  skills: [],
  certifications: [],
  availability: [],
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

function sentBody() {
  return JSON.parse(fetchMock.mock.calls[0][1]?.body as string)
}

describe('BasicProfileForm', () => {
  it('keeps the radius within 1 … 500 km', async () => {
    renderWithProviders(<BasicProfileForm profile={profile} />)
    const radius = screen.getByLabelText('Search radius, km')

    await userEvent.type(radius, '600')
    await userEvent.tab()

    expect(radius).toHaveValue('500')
  })

  it('sends the picked point, an upper-case country code and leaves out an empty radius', async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({ ...profile, lat: 50.2, lon: 14.3, countryCode: 'CZ' })))
    renderWithProviders(<BasicProfileForm profile={profile} />)

    clickMap(50.2, 14.3)
    await userEvent.type(screen.getByLabelText('Country code'), 'cz')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/volunteers/me')
    expect(fetchMock.mock.calls[0][1]?.method).toBe('PUT')
    expect(sentBody()).toMatchObject({ lat: 50.2, lon: 14.3, countryCode: 'CZ', firstName: 'Lucie', maxActiveAssignments: 1 })
    expect(sentBody()).not.toHaveProperty('radius')
  })

  it('sends no coordinates when the profile has no point (Q71: the location is never cleared)', async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify(profile)))
    renderWithProviders(<BasicProfileForm profile={profile} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(sentBody()).not.toHaveProperty('lat')
    expect(sentBody()).not.toHaveProperty('lon')
  })

  it('shows the field errors of the backend next to the fields', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Validation failed', errors: { lastName: 'Last name must not be blank' } }), {
        status: 400,
      }),
    )
    renderWithProviders(<BasicProfileForm profile={profile} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Last name must not be blank')).toBeInTheDocument()
    expect(screen.getByText('Validation failed')).toBeInTheDocument()
  })
})
