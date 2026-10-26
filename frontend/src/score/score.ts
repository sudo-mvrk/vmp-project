import type { Score } from '../api/types'

// Q83: the score is shown as the backend sends it; rounding to two decimals is only formatting.
export const formatScore = (value: number) => value.toFixed(2)

// Mantine's Progress takes 0 … 100. The backend keeps every component in [0, 1];
// the clamp only stops a broken response from drawing outside the bar.
export const toPercent = (value: number) => Math.min(100, Math.max(0, value * 100))

export type ScoreComponent = {
  key: 'geo' | 'skill' | 'priority'
  label: string
  symbol: 'α' | 'β' | 'γ'
  weight: number
  value: number
}

// The three components of FUNCTION.md with the weight each one had in this score (Q62).
export function componentsOf(score: Score): ScoreComponent[] {
  const { alpha, beta, gamma } = score.weights
  return [
    { key: 'geo', label: 'Distance', symbol: 'α', weight: alpha, value: score.geo },
    { key: 'skill', label: 'Skills', symbol: 'β', weight: beta, value: score.skill },
    { key: 'priority', label: 'Priority', symbol: 'γ', weight: gamma, value: score.priority },
  ]
}
