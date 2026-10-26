import { Center, Loader, Paper, Stack, Text } from '@mantine/core'
import { useState } from 'react'
import { notifications } from '@mantine/notifications'
import { useNavigate, useParams } from 'react-router'
import { useActivity } from '../activity/queries'
import { ErrorAlert } from '../common/ErrorAlert'
import { PageHeader } from '../common/PageHeader'
import { ActivityForm } from './ActivityForm'
import { valuesOf } from './activityForm'
import { actionsOf } from './activityStatus'
import { useUpdateActivity } from './queries'

export function EditActivityPage() {
  const id = Number(useParams().id)
  const [now] = useState(() => new Date())
  const activity = useActivity(id)
  const update = useUpdateActivity(id)
  const navigate = useNavigate()

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
  return (
    <Stack maw={880} mx="auto" w="100%" gap="lg">
      <PageHeader eyebrow="Edit activity" title={data.title} />
      {actionsOf(data, now).includes('edit') ? (
        <Paper withBorder p="md">
          <ActivityForm
            initial={valuesOf(data)}
            submitLabel="Save"
            openNote={data.status === 'OPEN'}
            pending={update.isPending}
            error={update.error}
            onSubmit={(request, { onError }) =>
              update.mutate(request, {
                onSuccess: () => {
                  notifications.show({ color: 'sage', message: 'Activity saved' })
                  navigate(`/organization/activities/${id}`)
                },
                onError,
              })
            }
          />
        </Paper>
      ) : (
        <Text>
          This activity is {data.status.toLowerCase()}
          {data.status === 'OPEN' ? ' and has started' : ''}, so it can no longer be edited.
        </Text>
      )}
    </Stack>
  )
}
