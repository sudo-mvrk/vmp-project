import { Anchor, Badge, Group, Stack, Text } from '@mantine/core'
import { Link } from 'react-router'
import type { Activity } from '../api/types'
import { formatWindow, placeText, spotsText } from './format'
import { priorityColor } from './labels'

// The part of an activity shown in lists: the public list and the recommendations (Q84).
export function ActivitySummary({ activity }: { activity: Activity }) {
  const place = placeText(activity)
  return (
    <Stack gap={4}>
      <Group gap="xs">
        <Anchor component={Link} to={`/activities/${activity.id}`} fw={600}>
          {activity.title}
        </Anchor>
        <Badge color={priorityColor[activity.priority]} variant="light" size="sm">
          {activity.priority}
        </Badge>
      </Group>
      <Text size="sm">{formatWindow(activity.startsAt, activity.endsAt)}</Text>
      <Text size="sm" c="dimmed">
        {[place, spotsText(activity)].filter(Boolean).join(' · ')}
      </Text>
      {activity.shortDescription && <Text size="sm">{activity.shortDescription}</Text>}
      {activity.requiredSkills.length > 0 && (
        <Group gap={4}>
          {activity.requiredSkills.map((skill) => (
            <Badge key={skill.id} variant="outline" size="xs">
              {skill.skillName}
            </Badge>
          ))}
        </Group>
      )}
    </Stack>
  )
}
