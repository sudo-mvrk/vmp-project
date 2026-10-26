import { Anchor, Badge, Button, Center, Group, Loader, Modal, Paper, Stack, Tabs, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, Navigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import type { Activity } from '../api/types'
import { formatWindow, placeText, spotsText } from '../activity/format'
import { priorityColor } from '../activity/labels'
import { useActivity } from '../activity/queries'
import { ErrorAlert } from '../common/ErrorAlert'
import { NotFoundPage } from '../pages/NotFoundPage'
import { actionsOf, statusColor } from './activityStatus'
import { ApplicationsTable } from './ApplicationsTable'
import { useActivityAction, useActivityApplications, useOwnOrganization, type StatusAction } from './queries'
import { RecommendedVolunteers } from './RecommendedVolunteers'

// Q96: cancel and complete cannot be undone and email the volunteers, so they are confirmed first.
const CONFIRM: Record<'cancel' | 'complete', { title: string; text: string; button: string; color: string }> = {
  cancel: {
    title: 'Cancel this activity?',
    text: 'It cannot be reopened. Volunteers who applied or were accepted get an email.',
    button: 'Cancel activity',
    color: 'red',
  },
  complete: {
    title: 'Mark this activity as completed?',
    text: 'It moves to the history of the accepted volunteers, and they get a thank-you email.',
    button: 'Complete',
    color: 'teal',
  },
}

// Q89, Q98: the owner's page of one activity, with its applications and recommended volunteers.
export function ManageActivityPage() {
  const id = Number(useParams().id)
  const activity = useActivity(id)
  const organization = useOwnOrganization()

  if (!Number.isInteger(id) || (activity.error instanceof ApiError && activity.error.status === 404)) {
    return <NotFoundPage />
  }
  if (activity.isPending || organization.isPending) {
    return (
      <Center mt="xl">
        <Loader />
      </Center>
    )
  }
  if (activity.isError || organization.isError) {
    return <ErrorAlert error={activity.error ?? organization.error} />
  }
  // Another organization's activity: only its public page.
  if (activity.data.organizationId !== organization.data.id) {
    return <Navigate to={`/activities/${id}`} replace />
  }
  return <ManageActivity activity={activity.data} />
}

function ManageActivity({ activity }: { activity: Activity }) {
  const action = useActivityAction(activity.id)
  // The same query as the table's, so the count costs no extra request. A draft has no applications.
  const applications = useActivityApplications(activity.id, activity.status !== 'DRAFT')
  const [confirming, setConfirming] = useState<'cancel' | 'complete' | null>(null)
  const [now] = useState(() => new Date())
  const actions = actionsOf(activity, now)
  const place = placeText(activity)

  const run = (statusAction: StatusAction) =>
    action.mutate(statusAction, {
      onSuccess: (saved) => {
        setConfirming(null)
        notifications.show({ color: 'teal', message: `Activity is now ${saved.status.toLowerCase()}` })
      },
      onError: () => setConfirming(null),
    })

  return (
    <Stack maw={860}>
      <Group justify="space-between" align="start">
        <div>
          <Group gap="xs">
            <Title order={2}>{activity.title}</Title>
            <Badge color={statusColor[activity.status]}>{activity.status}</Badge>
            <Badge color={priorityColor[activity.priority]} variant="light">
              {activity.priority}
            </Badge>
          </Group>
          {activity.status !== 'DRAFT' && (
            <Anchor component={Link} to={`/activities/${activity.id}`} size="sm">
              Public page
            </Anchor>
          )}
        </div>
        <Group gap="xs">
          {actions.includes('edit') && (
            <Button variant="default" component={Link} to={`/organization/activities/${activity.id}/edit`}>
              Edit
            </Button>
          )}
          {actions.includes('publish') && (
            <Button onClick={() => run('publish')} loading={action.isPending && action.variables === 'publish'}>
              Publish
            </Button>
          )}
          {actions.includes('complete') && (
            <Button color="teal" onClick={() => setConfirming('complete')}>
              Complete
            </Button>
          )}
          {actions.includes('cancel') && (
            <Button color="red" variant="light" onClick={() => setConfirming('cancel')}>
              Cancel
            </Button>
          )}
        </Group>
      </Group>

      <ErrorAlert error={action.error} />

      <Paper withBorder p="md">
        <Stack gap="xs">
          <Text>{formatWindow(activity.startsAt, activity.endsAt)}</Text>
          {place && <Text size="sm">{place}</Text>}
          <Text size="sm">
            {spotsText(activity)} ({activity.acceptedCount} accepted)
          </Text>
          <Text size="sm">
            Required skills: {activity.requiredSkills.map((skill) => skill.skillName).join(', ') || 'none'}
          </Text>
          <Text size="sm">
            Required certifications:{' '}
            {activity.requiredCertifications.map((certification) => certification.name).join(', ') || 'none'}
          </Text>
          <Text size="sm" data-testid="weights">
            {activity.weights
              ? `Custom weights: α ${activity.weights.alpha.toFixed(2)} · β ${activity.weights.beta.toFixed(2)} · γ ${activity.weights.gamma.toFixed(2)}`
              : "Platform's weights"}
          </Text>
        </Stack>
      </Paper>

      {(activity.fullDescription || activity.shortDescription) && (
        <Text style={{ whiteSpace: 'pre-line' }}>{activity.fullDescription || activity.shortDescription}</Text>
      )}

      {activity.status !== 'DRAFT' && (
        <Tabs defaultValue="applications" keepMounted={false}>
          <Tabs.List>
            <Tabs.Tab value="applications">
              Applications{applications.data ? ` (${applications.data.length})` : ''}
            </Tabs.Tab>
            <Tabs.Tab value="recommended">Recommended volunteers</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="applications" pt="md">
            <ApplicationsTable activity={activity} now={now} />
          </Tabs.Panel>
          <Tabs.Panel value="recommended" pt="md">
            <RecommendedVolunteers activity={activity} now={now} />
          </Tabs.Panel>
        </Tabs>
      )}

      <Modal opened={confirming !== null} onClose={() => setConfirming(null)} title={confirming && CONFIRM[confirming].title}>
        {confirming && (
          <Stack>
            <Text size="sm">{CONFIRM[confirming].text}</Text>
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setConfirming(null)}>
                Back
              </Button>
              <Button color={CONFIRM[confirming].color} loading={action.isPending} onClick={() => run(confirming)}>
                {CONFIRM[confirming].button}
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}
