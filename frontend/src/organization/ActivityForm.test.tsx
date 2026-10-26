import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { activity, json } from '../test/fixtures'
import { stubFetch } from '../test/fetchRoutes'
import { renderWithProviders } from '../test/render'
import { ActivityForm } from './ActivityForm'
import { valuesOf } from './activityForm'

vi.mock('react-leaflet', () => import('../test/leafletMock'))

beforeEach(() => {
  stubFetch({ 'GET /skills': () => json([]), 'GET /certifications': () => json([]) })
})

afterEach(() => vi.unstubAllGlobals())

function renderForm(onSubmit = vi.fn(), error: Error | null = null) {
  renderWithProviders(
    <ActivityForm initial={valuesOf(activity())} submitLabel="Save" onSubmit={onSubmit} pending={false} error={error} />,
  )
  return onSubmit
}

describe('ActivityForm', () => {
  it('submits the whole activity without weights while the override is off', async () => {
    const onSubmit = renderForm()

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      title: 'Sort food donations',
      startsAt: '2026-11-07T09:00',
      endsAt: '2026-11-07T12:00',
      priority: 'HIGH',
      weights: undefined,
    })
  })

  it('shows the weights with their sum when switched on and blocks a sum other than 1', async () => {
    const onSubmit = renderForm()

    await userEvent.click(screen.getByRole('switch', { name: /Custom weights/ }))
    expect(screen.getByTestId('weights-sum')).toHaveTextContent('Sum: 1.00')

    const gamma = screen.getByLabelText('Priority (γ)')
    await userEvent.clear(gamma)
    await userEvent.type(gamma, '0.5')
    expect(screen.getByTestId('weights-sum')).toHaveTextContent('Sum: 1.30')

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('α + β + γ must be 1')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('sends the override when the sum is 1', async () => {
    const onSubmit = renderForm()

    await userEvent.click(screen.getByRole('switch', { name: /Custom weights/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0][0].weights).toEqual({ alpha: 0.4, beta: 0.4, gamma: 0.2 })
  })

  it('puts the field errors of a backend 400 on the inputs and the rest above the form', async () => {
    const onSubmit = vi.fn((_request, { onError }) =>
      onError(new ApiError(400, 'Validation failed', { spotsNeeded: 'An activity needs at least 1 spot' })),
    )
    renderForm(onSubmit, new ApiError(400, 'An activity must start in the future, got 2026-10-01T09:00'))

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('An activity needs at least 1 spot')).toBeInTheDocument()
    expect(screen.getByText('An activity must start in the future, got 2026-10-01T09:00')).toBeInTheDocument()
  })
})
