import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import { score } from '../test/fixtures'
import { ScoreBreakdown } from './ScoreBreakdown'

function row(key: string) {
  return within(screen.getByTestId(`score-${key}`))
}

function barValue(key: string) {
  return Number(row(key).getByRole('progressbar').getAttribute('aria-valuenow'))
}

describe('ScoreBreakdown', () => {
  it('shows each component as a bar in [0, 1] with its weight', () => {
    renderWithProviders(<ScoreBreakdown score={score()} />)

    expect(row('geo').getByText(/Distance/)).toHaveTextContent('Distance (α 0.40)')
    expect(row('geo').getByText('0.79')).toBeInTheDocument()
    expect(barValue('geo')).toBeCloseTo(79.356)
    expect(row('skill').getByText(/Skills/)).toHaveTextContent('Skills (β 0.40)')
    expect(barValue('skill')).toBe(100)
    expect(row('priority').getByText(/Priority/)).toHaveTextContent('Priority (γ 0.20)')
    expect(barValue('priority')).toBe(0)
  })

  it('shows the total as given, never recomputed from the components', () => {
    // 0.4 * 0.5 + 0.4 * 0.5 + 0.2 * 0.5 = 0.5, but the backend's total is what counts.
    renderWithProviders(<ScoreBreakdown score={score({ geo: 0.5, skill: 0.5, priority: 0.5, total: 0.123 })} />)

    expect(screen.getByTestId('score-total')).toHaveTextContent('0.12')
  })

  it('shows the weights of the score, not fixed ones', () => {
    renderWithProviders(
      <ScoreBreakdown score={score({ weights: { alpha: 0, beta: 1, gamma: 0, halfDistanceKm: 5 } })} />,
    )

    expect(row('geo').getByText(/Distance/)).toHaveTextContent('Distance (α 0.00)')
    expect(row('skill').getByText(/Skills/)).toHaveTextContent('Skills (β 1.00)')
  })
})
