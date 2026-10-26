import { Anchor, Badge, Button, Center, Loader, SegmentedControl, Stack, Table, Text, Paper } from '@mantine/core'
import { useState } from 'react'
import { IconPlus } from '@tabler/icons-react'
import { Link } from 'react-router'
import { formatWindow } from '../activity/format'
import { ErrorAlert } from '../common/ErrorAlert'
import { PageHeader } from '../common/PageHeader'
import { matchesFilter, statusColor, type StatusFilter } from './activityStatus'
import { useMyActivities } from './queries'

// Q88: the organization's own activities in every status, soonest first, filtered on the client.
export function ActivitiesPage() {
  const [filter, setFilter] = useState<StatusFilter>('active')
  const activities = useMyActivities()
  const shown = (activities.data ?? []).filter((activity) => matchesFilter(activity, filter))

  return (
    <Stack maw={1080} mx="auto" w="100%" gap="lg">
      <PageHeader
        eyebrow="Organization"
        title="Our activities"
        description="Drafts, open and finished activities of your organization."
        actions={
          <Button component={Link} to="/organization/activities/new" leftSection={<IconPlus size={16} />}>
            New activity
          </Button>
        }
      />
      <SegmentedControl
        w="fit-content"
        value={filter}
        onChange={(value) => setFilter(value as StatusFilter)}
        data={[
          { value: 'active', label: 'Active' },
          { value: 'finished', label: 'Finished' },
          { value: 'all', label: 'All' },
        ]}
      />
      {activities.isPending && (
        <Center mt="xl">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={activities.error} />
      {activities.data && shown.length === 0 && <Text c="dimmed">No activities here.</Text>}
      {shown.length > 0 && (
        <Paper withBorder radius="lg" style={{ overflow: 'hidden' }}>
          <Table.ScrollContainer minWidth={640} type="native">
            <Table>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Title</Table.Th>
                  <Table.Th>When</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th>Accepted</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {shown.map((activity) => (
                  <Table.Tr key={activity.id}>
                    <Table.Td>
                      <Anchor component={Link} to={`/organization/activities/${activity.id}`}>
                        {activity.title}
                      </Anchor>
                    </Table.Td>
                    <Table.Td>{formatWindow(activity.startsAt, activity.endsAt)}</Table.Td>
                    <Table.Td>
                      <Badge color={statusColor[activity.status]} variant="light">
                        {activity.status}
                      </Badge>
                    </Table.Td>
                    <Table.Td>
                      {activity.acceptedCount} / {activity.spotsNeeded}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Paper>
      )}
    </Stack>
  )
}
