import { Anchor, Badge, Group, Stack, Text } from '@mantine/core'
import { IconCalendarEvent, IconMapPin, IconUsers } from '@tabler/icons-react'
import { Link } from 'react-router'
import type { Activity } from '../api/types'
import { formatWindow, placeText, spotsText } from './format'
import { priorityColor } from './labels'

const ICON = { size: 15, stroke: 1.5, color: 'var(--mantine-color-navy-4)' }

// The part of an activity shown in lists: the public list and the recommendations (Q84).
export function ActivitySummary({ activity }: { activity: Activity }) {
  const place = placeText(activity)
  return (
    <Stack gap={8}>
      <Group gap="xs" wrap="nowrap" align="flex-start">
        <Anchor component={Link} to={`/activities/${activity.id}`} fw={700} size="lg" c="navy.9" lh={1.3}>
          {activity.title}
        </Anchor>
        <Badge color={priorityColor[activity.priority]} size="sm" mt={3}>
          {activity.priority}
        </Badge>
      </Group>
      <Group gap="lg" wrap="wrap">
        <Group gap={6} wrap="nowrap">
          <IconCalendarEvent {...ICON} />
          <Text size="sm">{formatWindow(activity.startsAt, activity.endsAt)}</Text>
        </Group>
        {place && (
          <Group gap={6} wrap="nowrap">
            <IconMapPin {...ICON} />
            <Text size="sm" c="dimmed">
              {place}
            </Text>
          </Group>
        )}
        <Group gap={6} wrap="nowrap">
          <IconUsers {...ICON} />
          <Text size="sm" c="dimmed">
            {spotsText(activity)}
          </Text>
        </Group>
      </Group>
      {activity.shortDescription && (
        <Text size="sm" c="navy.8">
          {activity.shortDescription}
        </Text>
      )}
      {activity.requiredSkills.length > 0 && (
        <Group gap={6}>
          {activity.requiredSkills.map((skill) => (
            <Badge key={skill.id} variant="outline" color="navy.4" size="sm" fw={500} tt="none" lts={0}>
              {skill.skillName}
            </Badge>
          ))}
        </Group>
      )}
    </Stack>
  )
}
