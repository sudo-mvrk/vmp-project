import { Badge, Center, Grid, Group, Loader, Paper, SimpleGrid, Stack, Text } from '@mantine/core'
import { IconCalendarEvent, IconMapPin, IconUsers } from '@tabler/icons-react'
import { useParams } from 'react-router'
import { AdminCancelButton } from '../admin/AdminCancelButton'
import { ApiError } from '../api/client'
import { roleOf, useCurrentUser } from '../auth/session'
import { ErrorAlert } from '../common/ErrorAlert'
import { PageHeader } from '../common/PageHeader'
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
    <Stack maw={1080} mx="auto" w="100%" gap="lg">
      <PageHeader
        eyebrow={organization.data?.organizationName ?? 'Activity'}
        title={data.title}
        badges={
          <>
            <Badge color={priorityColor[data.priority]}>{data.priority}</Badge>
            {data.status !== 'OPEN' && <Badge color="gray">{data.status}</Badge>}
          </>
        }
      />

      <Grid gap="xl">
        <Grid.Col span={{ base: 12, md: 7 }} order={{ base: 2, md: 1 }}>
          <Stack gap="xl">
            {(data.fullDescription || data.shortDescription) && (
              <Text size="md" lh={1.75} c="navy.9" style={{ whiteSpace: 'pre-line' }}>
                {data.fullDescription || data.shortDescription}
              </Text>
            )}
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <Requirement title="Required skills" items={data.requiredSkills.map((s) => ({ id: s.id, name: s.skillName }))} />
              <Requirement title="Required certifications" items={data.requiredCertifications} />
            </SimpleGrid>
            <Paper withBorder radius="lg" style={{ overflow: 'hidden' }}>
              <PointMap point={{ lat: data.lat, lon: data.lon }} />
            </Paper>
          </Stack>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 5 }} order={{ base: 1, md: 2 }}>
          <Paper withBorder p="xl" shadow="sm" style={{ position: 'sticky', top: 92 }}>
            <Stack gap="md">
              <Fact icon={<IconCalendarEvent {...ICON} />} text={formatWindow(data.startsAt, data.endsAt)} />
              {place && <Fact icon={<IconMapPin {...ICON} />} text={place} />}
              <Fact icon={<IconUsers {...ICON} />} text={spotsText(data)} />
              {/* Wait for the own applications, so "Apply" does not flash before "already applied". */}
              {(role !== 'VOLUNTEER' || !myApplications.isPending) && (
                <Group mt="xs">
                  <ApplyButton activityId={data.id} state={applyStateOf(role, data, myApplications.data)} />
                </Group>
              )}
              {role === 'ADMIN' && data.status === 'OPEN' && (
                <Group>
                  <AdminCancelButton activityId={data.id} />
                </Group>
              )}
            </Stack>
          </Paper>
        </Grid.Col>
      </Grid>
    </Stack>
  )
}

const ICON = { size: 18, stroke: 1.5, color: 'var(--mantine-color-navy-4)' }

function Fact({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <Group gap="sm" wrap="nowrap">
      {icon}
      <Text>{text}</Text>
    </Group>
  )
}

function Requirement({ title, items }: { title: string; items: { id: number; name: string }[] }) {
  return (
    <div>
      <Text size="xs" fw={600} tt="uppercase" lts="0.14em" c="navy.5" mb={8}>
        {title}
      </Text>
      {items.length === 0 ? (
        <Text size="sm" c="dimmed">
          None
        </Text>
      ) : (
        <Group gap={6}>
          {items.map((item) => (
            <Badge key={item.id} variant="outline" color="navy.4" fw={500} tt="none" lts={0}>
              {item.name}
            </Badge>
          ))}
        </Group>
      )}
    </div>
  )
}
