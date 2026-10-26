import { Center, Loader, Pagination, Paper, Stack, Text, Title } from '@mantine/core'
import { useState } from 'react'
import { ErrorAlert } from '../common/ErrorAlert'
import { ActivitySummary } from './ActivitySummary'
import { useActivities } from './queries'

// Q84: the public list for everyone, without filters (the backend has none), 20 per page.
export function ActivityListPage() {
  const [page, setPage] = useState(0)
  const activities = useActivities(page)

  return (
    <Stack maw={860}>
      <Title order={2}>Activities</Title>
      {activities.isPending && (
        <Center mt="xl">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={activities.error} />
      {activities.data && (
        <>
          {activities.data.content.length === 0 && <Text c="dimmed">No open activities at the moment.</Text>}
          {activities.data.content.map((activity) => (
            <Paper key={activity.id} withBorder p="md">
              <ActivitySummary activity={activity} />
            </Paper>
          ))}
          {activities.data.totalPages > 1 && (
            // Mantine counts pages from 1, the backend from 0.
            <Pagination total={activities.data.totalPages} value={page + 1} onChange={(p) => setPage(p - 1)} />
          )}
        </>
      )}
    </Stack>
  )
}
