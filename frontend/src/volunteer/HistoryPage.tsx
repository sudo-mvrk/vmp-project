import { Anchor, Center, Loader, Paper, Stack, Text } from '@mantine/core'
import { Link } from 'react-router'
import { formatWindow } from '../activity/format'
import { ErrorAlert } from '../common/ErrorAlert'
import { PageHeader } from '../common/PageHeader'
import { useHistory } from './queries'

// Q113: completed activities the volunteer took part in, in the backend's order (newest first).
export function HistoryPage() {
  const history = useHistory()

  return (
    <Stack maw={880} mx="auto" w="100%" gap="lg">
      <PageHeader eyebrow="Volunteer" title="History" description="Completed activities you took part in." />
      {history.isPending && (
        <Center mt="xl">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={history.error} />
      {history.data && (
        <>
          <Text c="dimmed" data-testid="history-count">
            {history.data.length === 1 ? '1 activity completed' : `${history.data.length} activities completed`}
          </Text>
          {history.data.length === 0 && (
            <Text>Activities appear here once the organization marks them as completed.</Text>
          )}
          {history.data.map((item) => (
            <Paper key={item.applicationId} withBorder p="md" data-testid="history-item">
              <Anchor component={Link} to={`/activities/${item.activityId}`} fw={600}>
                {item.title}
              </Anchor>
              <Text size="sm">{formatWindow(item.startsAt, item.endsAt)}</Text>
              <Text size="sm" c="dimmed">
                {[item.organizationName, item.city].filter(Boolean).join(' · ')}
              </Text>
            </Paper>
          ))}
        </>
      )}
    </Stack>
  )
}
