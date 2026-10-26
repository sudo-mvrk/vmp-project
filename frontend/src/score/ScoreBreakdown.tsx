import { Group, Progress, Stack, Text, Tooltip } from '@mantine/core'
import type { Score } from '../api/types'
import { componentsOf, formatScore, toPercent } from './score'
import classes from './ScoreBreakdown.module.css'

// Q62, Q83: the three components as bars in [0, 1], the weight of each and the total, exactly as the
// backend computed them. The frontend never recomputes a score.
export function ScoreBreakdown({ score }: { score: Score }) {
  return (
    <div className={classes.panel}>
      <Group justify="space-between" align="flex-end" mb="md">
        <Text size="xs" fw={600} tt="uppercase" lts="0.14em" className={classes.weight}>
          Match score
        </Text>
        <Text className={classes.total} data-testid="score-total">
          {formatScore(score.total)}
        </Text>
      </Group>
      <Stack gap={10}>
        {componentsOf(score).map((component) => {
          const label = (
            <Text size="xs" className={classes.label}>
              {component.label}{' '}
              <span className={classes.weight}>
                ({component.symbol} {formatScore(component.weight)})
              </span>
            </Text>
          )
          return (
            <div key={component.key} data-testid={`score-${component.key}`}>
              <Group justify="space-between" gap="xs" mb={4}>
                {component.key === 'geo' ? (
                  <Tooltip label={`Distance score is 0.50 at ${score.weights.halfDistanceKm} km`}>{label}</Tooltip>
                ) : (
                  label
                )}
                <Text size="xs" fw={700} c="navy.9">
                  {formatScore(component.value)}
                </Text>
              </Group>
              <Progress
                value={toPercent(component.value)}
                size={7}
                radius="xl"
                color="navy.7"
                bg="rgba(17, 31, 61, 0.08)"
                aria-label={component.label}
              />
            </div>
          )
        })}
      </Stack>
    </div>
  )
}
