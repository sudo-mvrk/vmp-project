import { Group, Progress, Stack, Text, Tooltip } from '@mantine/core'
import type { Score } from '../api/types'
import { componentsOf, formatScore, toPercent } from './score'

// Q62, Q83: the three components as bars in [0, 1], the weight of each and the total, exactly as the
// backend computed them. The frontend never recomputes a score.
export function ScoreBreakdown({ score }: { score: Score }) {
  return (
    <Stack gap={6}>
      {componentsOf(score).map((component) => (
        <div key={component.key} data-testid={`score-${component.key}`}>
          <Group justify="space-between" gap="xs">
            {component.key === 'geo' ? (
              <Tooltip label={`Distance score is 0.50 at ${score.weights.halfDistanceKm} km`}>
                <Text size="xs">
                  {component.label} ({component.symbol} {formatScore(component.weight)})
                </Text>
              </Tooltip>
            ) : (
              <Text size="xs">
                {component.label} ({component.symbol} {formatScore(component.weight)})
              </Text>
            )}
            <Text size="xs" fw={600}>
              {formatScore(component.value)}
            </Text>
          </Group>
          <Progress value={toPercent(component.value)} size="sm" aria-label={component.label} />
        </div>
      ))}
      <Group justify="space-between">
        <Text size="sm" fw={600}>
          Total
        </Text>
        <Text size="sm" fw={700} data-testid="score-total">
          {formatScore(score.total)}
        </Text>
      </Group>
    </Stack>
  )
}
