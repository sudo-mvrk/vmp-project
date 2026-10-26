import { Anchor, Badge, Button, Center, Grid, Group, Loader, Paper, Select, Stack, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import type { Activity, VolunteerProfile } from '../api/types'
import { freeSpots } from '../activity/format'
import { ErrorAlert } from '../common/ErrorAlert'
import { ScoreBreakdown } from '../score/ScoreBreakdown'
import { canRecommend, fullName } from './applications'
import { useInvite, useRecommendedVolunteers } from './queries'
import { VolunteerProfileModal } from './VolunteerProfileModal'

const LIMITS = ['10', '25', '50']

// Q103 … Q105: the best volunteers for this activity, in the backend's order, each with an invite.
export function RecommendedVolunteers({ activity, now }: { activity: Activity; now: Date }) {
  const [limit, setLimit] = useState('10')
  const [shown, setShown] = useState<VolunteerProfile | null>(null)
  const enabled = canRecommend(activity, now)
  const recommendations = useRecommendedVolunteers(activity.id, Number(limit), enabled)
  const invite = useInvite(activity.id)

  if (!enabled) {
    return <Text c="dimmed">Recommendations are available for open activities that have not started.</Text>
  }

  const onInvite = (volunteer: VolunteerProfile) =>
    invite.mutate(volunteer.id, {
      onSuccess: () => notifications.show({ color: 'teal', message: `${fullName(volunteer)} is invited` }),
      onError: (e) => notifications.show({ color: 'red', title: 'Could not invite', message: e.message }),
    })

  return (
    <Stack>
      <Group justify="space-between" align="end">
        <Text size="sm" c="dimmed">
          {activity.weights
            ? "Ranked with this activity's custom weights."
            : "Ranked with the platform's weights."}
        </Text>
        <Select label="Show" data={LIMITS} value={limit} onChange={(v) => v && setLimit(v)} w={90} allowDeselect={false} />
      </Group>
      {recommendations.isPending && (
        <Center mt="md">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={recommendations.error} />
      {recommendations.data?.length === 0 && (
        <Text c="dimmed">
          {freeSpots(activity) === 0
            ? 'No free spots: recommendations and invitations need one.'
            : 'No volunteer fits this activity right now (availability, certifications, capacity).'}
        </Text>
      )}
      {recommendations.data?.map(({ volunteer, score }, index) => (
        <Paper key={volunteer.id} withBorder p="md" data-testid="recommended-volunteer">
          <Grid>
            <Grid.Col span={{ base: 12, sm: 7 }}>
              <Stack gap={4}>
                <Group gap="xs">
                  <Text fw={700} c="dimmed">
                    {index + 1}.
                  </Text>
                  <Anchor component="button" type="button" fw={600} onClick={() => setShown(volunteer)}>
                    {fullName(volunteer)}
                  </Anchor>
                </Group>
                {volunteer.city && (
                  <Text size="sm" c="dimmed">
                    {volunteer.city}
                  </Text>
                )}
                {volunteer.skills.length > 0 && (
                  <Group gap={4}>
                    {volunteer.skills.map((skill) => (
                      <Badge key={skill.id} variant="outline" size="xs">
                        {skill.skillName}
                      </Badge>
                    ))}
                  </Group>
                )}
                <Group mt="xs">
                  <Button
                    size="xs"
                    loading={invite.isPending && invite.variables === volunteer.id}
                    onClick={() => onInvite(volunteer)}
                  >
                    Invite
                  </Button>
                </Group>
              </Stack>
            </Grid.Col>
            <Grid.Col span={{ base: 12, sm: 5 }}>
              <ScoreBreakdown score={score} />
            </Grid.Col>
          </Grid>
        </Paper>
      ))}
      <VolunteerProfileModal volunteer={shown} onClose={() => setShown(null)} />
    </Stack>
  )
}
