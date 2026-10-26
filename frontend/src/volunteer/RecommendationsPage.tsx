import { Alert, Anchor, Center, Grid, Group, List, Loader, Paper, Select, Stack, Text, Title } from '@mantine/core'
import { useState } from 'react'
import { Link } from 'react-router'
import { ActivitySummary } from '../activity/ActivitySummary'
import { ApplyButton } from '../activity/ApplyButton'
import { useRecommendedActivities } from '../activity/queries'
import { ErrorAlert } from '../common/ErrorAlert'
import { ScoreBreakdown } from '../score/ScoreBreakdown'
import { useOwnVolunteerProfile } from './queries'

// Q82: the endpoint returns a top N (1 … 50), no pages.
const LIMITS = ['10', '25', '50']

// Recommended activities in the order of the backend (best first), each with its score breakdown (Q62).
export function RecommendationsPage() {
  const [limit, setLimit] = useState('10')
  const recommendations = useRecommendedActivities(Number(limit))
  const profile = useOwnVolunteerProfile()
  const noLocation = profile.data !== undefined && profile.data.lat === null

  return (
    <Stack maw={960}>
      <Group justify="space-between" align="end">
        <Title order={2}>Recommended for me</Title>
        <Select label="Show" data={LIMITS} value={limit} onChange={(v) => v && setLimit(v)} w={90} allowDeselect={false} />
      </Group>
      {noLocation && (
        // Q81: without a point every activity gets distance score 0.
        <Alert color="amber">
          Your profile has no location, so distance does not count.{' '}
          <Anchor component={Link} to="/volunteer/profile">
            Set your location
          </Anchor>
        </Alert>
      )}
      {recommendations.isPending && (
        <Center mt="xl">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={recommendations.error} />
      {recommendations.data?.length === 0 && <EmptyRecommendations />}
      {recommendations.data?.map(({ activity, score }, index) => (
        <Paper key={activity.id} withBorder p="md" data-testid="recommendation">
          <Grid>
            <Grid.Col span={{ base: 12, sm: 7 }}>
              <Stack gap="sm">
                <Group gap="xs" align="start" wrap="nowrap">
                  <Text fw={700} c="dimmed">
                    {index + 1}.
                  </Text>
                  <ActivitySummary activity={activity} />
                </Group>
                <Group>
                  {/* The backend leaves out activities the volunteer applied to (Q15), so every row can apply. */}
                  <ApplyButton activityId={activity.id} state={{ kind: 'apply' }} />
                </Group>
              </Stack>
            </Grid.Col>
            <Grid.Col span={{ base: 12, sm: 5 }}>
              <ScoreBreakdown score={score} />
            </Grid.Col>
          </Grid>
        </Paper>
      ))}
    </Stack>
  )
}

// Q81: the reasons an activity is left out, without guessing which one applies.
function EmptyRecommendations() {
  return (
    <Paper withBorder p="md">
      <Text fw={600}>No activity fits your profile right now.</Text>
      <Text size="sm" mt="xs">
        An activity is recommended only when:
      </Text>
      <List size="sm" mt={4}>
        <List.Item>your availability on its weekday covers its whole time, with no accepted activity at that time,</List.Item>
        <List.Item>you hold its required certifications, valid on its date,</List.Item>
        <List.Item>you have capacity left for another assignment,</List.Item>
        <List.Item>it lies within your search radius,</List.Item>
        <List.Item>it has a free spot and you have not applied to it yet.</List.Item>
      </List>
      <Group mt="sm">
        <Anchor component={Link} to="/volunteer/profile" size="sm">
          Edit your profile
        </Anchor>
        <Anchor component={Link} to="/activities" size="sm">
          See all activities
        </Anchor>
      </Group>
    </Paper>
  )
}
