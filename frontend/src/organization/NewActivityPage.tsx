import { Center, Loader, Paper, Stack, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useNavigate } from 'react-router'
import { ActivityForm } from './ActivityForm'
import { newValues } from './activityForm'
import { useCreateActivity, useOwnOrganization } from './queries'

// Q90: a new activity is saved as a draft; it is published from its page.
export function NewActivityPage() {
  const organization = useOwnOrganization()
  const create = useCreateActivity()
  const navigate = useNavigate()

  // Waits for the profile, so the form starts at the organization's place (Q94). Without it, it starts empty.
  if (organization.isPending) {
    return (
      <Center mt="xl">
        <Loader />
      </Center>
    )
  }
  return (
    <Stack maw={860}>
      <Title order={2}>New activity</Title>
      <Paper withBorder p="md">
        <ActivityForm
          initial={newValues(organization.data)}
          submitLabel="Save draft"
          pending={create.isPending}
          error={create.error}
          onSubmit={(request, { onError }) =>
            create.mutate(request, {
              onSuccess: (activity) => {
                notifications.show({ color: 'teal', message: 'Draft saved. Publish it when it is ready.' })
                navigate(`/organization/activities/${activity.id}`)
              },
              onError,
            })
          }
        />
      </Paper>
    </Stack>
  )
}
