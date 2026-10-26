import { Badge, Center, Group, Loader, Paper, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useParams } from 'react-router'
import { ApiError } from '../api/client'
import { roleOf, useCurrentUser } from '../auth/session'
import { ErrorAlert } from '../common/ErrorAlert'
import { PointMap } from '../map/PointMap'
import { NotFoundPage } from '../pages/NotFoundPage'
import { applyStateOf } from './apply'
import { ApplyButton } from './ApplyButton'
import { formatWindow, placeText, spotsText } from './format'
import { priorityColor } from './labels'
import { useActivity, useMyApplications, useOrganization } from './queries'

// Q85: one activity for everyone; a volunteer can apply from here (Q79).
export function ActivityPage() {
  const id = Number(useParams().id)
  const { data: user } = useCurrentUser()
  const role = user ? roleOf(user) : null
  const activity = useActivity(id)
  const organization = useOrganization(activity.data?.organizationId)
  const myApplications = useMyApplications(role === 'VOLUNTEER')

  // A draft of another organization is a 404 too (Q4e).
  if (!Number.isInteger(id) || (activity.error instanceof ApiError && activity.error.status === 404)) {
    return <NotFoundPage />
  }
  if (activity.isPending) {
    return (
      <Center mt="xl">
        <Loader />
      </Center>
    )
  }
  if (activity.isError) {
    return <ErrorAlert error={activity.error} />
  }

  const data = activity.data
  const place = placeText(data)
  return (
    <Stack maw={860}>
      <div>
        <Group gap="xs">
          <Title order={2}>{data.title}</Title>
          <Badge color={priorityColor[data.priority]} variant="light">
            {data.priority}
          </Badge>
          {data.status !== 'OPEN' && <Badge color="gray">{data.status}</Badge>}
        </Group>
        <Text c="dimmed">{organization.data?.organizationName ?? ' '}</Text>
      </div>

      <Paper withBorder p="md">
        <Stack gap="xs">
          <Text>{formatWindow(data.startsAt, data.endsAt)}</Text>
          {place && <Text size="sm">{place}</Text>}
          <Text size="sm">{spotsText(data)}</Text>
          {/* Wait for the own applications, so "Apply" does not flash before "already applied". */}
          {(role !== 'VOLUNTEER' || !myApplications.isPending) && (
            <Group>
              <ApplyButton activityId={data.id} state={applyStateOf(role, data, myApplications.data)} />
            </Group>
          )}
        </Stack>
      </Paper>

      {(data.fullDescription || data.shortDescription) && (
        <Text style={{ whiteSpace: 'pre-line' }}>{data.fullDescription || data.shortDescription}</Text>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <div>
          <Text fw={600} size="sm" mb={4}>
            Required skills
          </Text>
          {data.requiredSkills.length === 0 ? (
            <Text size="sm" c="dimmed">
              None
            </Text>
          ) : (
            <Group gap={4}>
              {data.requiredSkills.map((skill) => (
                <Badge key={skill.id} variant="outline">
                  {skill.skillName}
                </Badge>
              ))}
            </Group>
          )}
        </div>
        <div>
          <Text fw={600} size="sm" mb={4}>
            Required certifications
          </Text>
          {data.requiredCertifications.length === 0 ? (
            <Text size="sm" c="dimmed">
              None
            </Text>
          ) : (
            <Group gap={4}>
              {data.requiredCertifications.map((certification) => (
                <Badge key={certification.id} variant="outline" color="grape">
                  {certification.name}
                </Badge>
              ))}
            </Group>
          )}
        </div>
      </SimpleGrid>

      <PointMap point={{ lat: data.lat, lon: data.lon }} />
    </Stack>
  )
}
