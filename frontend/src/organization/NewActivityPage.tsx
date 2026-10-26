import { Center, Loader, Paper, Stack } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useNavigate } from 'react-router'
import { PageHeader } from '../common/PageHeader'
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
    <Stack maw={880} mx="auto" w="100%" gap="lg">
      <PageHeader eyebrow="Organization" title="New activity" description="It is saved as a draft; publish it from its page when it is ready." />
      <Paper withBorder p="xl">
        <ActivityForm
          initial={newValues(organization.data)}
          submitLabel="Save draft"
          pending={create.isPending}
          error={create.error}
          onSubmit={(request, { onError }) =>
            create.mutate(request, {
              onSuccess: (activity) => {
                notifications.show({ color: 'sage', message: 'Draft saved. Publish it when it is ready.' })
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
