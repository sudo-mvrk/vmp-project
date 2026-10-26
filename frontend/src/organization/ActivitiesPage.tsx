import { Anchor, Badge, Button, Center, Group, Loader, SegmentedControl, Stack, Table, Text, Title } from '@mantine/core'
import { useState } from 'react'
import { Link } from 'react-router'
import { formatWindow } from '../activity/format'
import { ErrorAlert } from '../common/ErrorAlert'
import { matchesFilter, statusColor, type StatusFilter } from './activityStatus'
import { useMyActivities } from './queries'

// Q88: the organization's own activities in every status, soonest first, filtered on the client.
export function ActivitiesPage() {
  const [filter, setFilter] = useState<StatusFilter>('active')
  const activities = useMyActivities()
  const shown = (activities.data ?? []).filter((activity) => matchesFilter(activity, filter))

  return (
    <Stack maw={960}>
      <Group justify="space-between">
        <Title order={2}>Our activities</Title>
        <Button component={Link} to="/organization/activities/new">
          New activity
        </Button>
      </Group>
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
        <Table striped highlightOnHover>
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
      )}
    </Stack>
  )
}
