import { Anchor, Badge, Button, Center, Group, Loader, Modal, SegmentedControl, Stack, Table, Text, Title, Tooltip } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link } from 'react-router'
import type { MyApplication } from '../api/types'
import { formatWindow } from '../activity/format'
import { applicationStatusColor } from '../activity/labels'
import { useMyApplications } from '../activity/queries'
import { ErrorAlert } from '../common/ErrorAlert'
import { isCurrent, ownActionsOf } from './myApplications'
import { useOwnApplicationAction, type OwnAction } from './queries'

type Confirming = { application: MyApplication; action: 'withdraw' | 'decline' }

// Q111: withdraw and decline are final (one application per volunteer and activity, Q5b).
function confirmText({ application, action }: Confirming): { title: string; text: string; button: string } {
  if (action === 'decline') {
    return {
      title: 'Decline this invitation?',
      text: `You cannot take part in "${application.activityTitle}" later.`,
      button: 'Decline',
    }
  }
  return {
    title: 'Withdraw from this activity?',
    text:
      application.status === 'ACCEPTED'
        ? `Your spot in "${application.activityTitle}" is freed, and you cannot apply again.`
        : `You cannot apply to "${application.activityTitle}" again.`,
    button: 'Withdraw',
  }
}

// Q108 … Q112: the volunteer's invitations and applications, with what can still be done with them.
export function ApplicationsPage() {
  const [filter, setFilter] = useState<'current' | 'past'>('current')
  const [now] = useState(() => new Date())
  const [confirming, setConfirming] = useState<Confirming | null>(null)
  const applications = useMyApplications(true)
  const act = useOwnApplicationAction()

  const run = (application: MyApplication, action: OwnAction) =>
    act.mutate(
      { application, action },
      {
        onSuccess: (saved) => {
          setConfirming(null)
          notifications.show({ color: 'sage', message: `${application.activityTitle}: ${saved.status.toLowerCase()}` })
        },
        onError: (e) => {
          setConfirming(null)
          notifications.show({ color: 'wine', title: 'Not done', message: e.message })
        },
      },
    )

  const shown = (applications.data ?? []).filter((application) => isCurrent(application) === (filter === 'current'))
  const invitations = shown.filter((application) => application.origin === 'INVITATION')
  const own = shown.filter((application) => application.origin === 'APPLICATION')

  const table = (rows: MyApplication[], testId: string) => (
    <Table striped data-testid={testId}>
      <Table.Tbody>
        {rows.map((application) => (
          <Row
            key={application.id}
            application={application}
            now={now}
            pending={act.isPending && act.variables?.application.id === application.id}
            onAccept={() => run(application, 'accept')}
            onConfirm={(action) => setConfirming({ application, action })}
          />
        ))}
      </Table.Tbody>
    </Table>
  )

  return (
    <Stack maw={960}>
      <Title order={2}>My applications</Title>
      <SegmentedControl
        w="fit-content"
        value={filter}
        onChange={(value) => setFilter(value as 'current' | 'past')}
        data={[
          { value: 'current', label: 'Current' },
          { value: 'past', label: 'Past' },
        ]}
      />
      {applications.isPending && (
        <Center mt="xl">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={applications.error} />
      {applications.data && (
        <>
          <Title order={4}>Invitations</Title>
          {invitations.length === 0 ? <Text c="dimmed">No invitations here.</Text> : table(invitations, 'invitations')}
          <Title order={4}>Applications</Title>
          {own.length === 0 ? (
            <Text c="dimmed">
              No applications here.{' '}
              <Anchor component={Link} to="/volunteer/recommendations">
                See recommended activities
              </Anchor>
            </Text>
          ) : (
            table(own, 'applications')
          )}
        </>
      )}

      <Modal opened={confirming !== null} onClose={() => setConfirming(null)} title={confirming && confirmText(confirming).title}>
        {confirming && (
          <Stack>
            <Text size="sm">{confirmText(confirming).text}</Text>
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setConfirming(null)}>
                Back
              </Button>
              <Button color="wine" loading={act.isPending} onClick={() => run(confirming.application, confirming.action)}>
                {confirmText(confirming).button}
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}

type RowProps = {
  application: MyApplication
  now: Date
  pending: boolean
  onAccept: () => void
  onConfirm: (action: 'withdraw' | 'decline') => void
}

function Row({ application, now, pending, onAccept, onConfirm }: RowProps) {
  const actions = ownActionsOf(application, now)
  return (
    <Table.Tr data-testid="own-application">
      <Table.Td>
        <Anchor component={Link} to={`/activities/${application.activityId}`}>
          {application.activityTitle}
        </Anchor>
        <Text size="xs" c="dimmed">
          {formatWindow(application.activityStartsAt, application.activityEndsAt)}
        </Text>
      </Table.Td>
      <Table.Td>
        <Group gap={4}>
          <Badge color={applicationStatusColor[application.status]} variant="light">
            {application.status}
          </Badge>
          {application.activityStatus !== 'OPEN' && (
            <Badge color="gray" variant="outline">
              Activity {application.activityStatus.toLowerCase()}
            </Badge>
          )}
        </Group>
      </Table.Td>
      <Table.Td>
        <Group gap="xs" justify="flex-end" wrap="nowrap">
          {actions.kind === 'invitation' && (
            <>
              <Tooltip label={actions.acceptBlocked} disabled={!actions.acceptBlocked}>
                {/* A disabled button gets no mouse events; the wrapper keeps the tooltip working. */}
                <span>
                  <Button size="xs" disabled={actions.acceptBlocked !== null} loading={pending} onClick={onAccept}>
                    Accept
                  </Button>
                </span>
              </Tooltip>
              <Button size="xs" color="wine" variant="light" onClick={() => onConfirm('decline')}>
                Decline
              </Button>
            </>
          )}
          {actions.kind === 'withdraw' && (
            <Button size="xs" color="wine" variant="light" onClick={() => onConfirm('withdraw')}>
              Withdraw
            </Button>
          )}
          {actions.kind === 'started' && (
            <Text size="xs" c="dimmed">
              Started: cannot withdraw
            </Text>
          )}
        </Group>
      </Table.Td>
    </Table.Tr>
  )
}
