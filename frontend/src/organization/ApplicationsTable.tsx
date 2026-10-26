import { Anchor, Badge, Button, Center, Group, Loader, Modal, Stack, Table, Text, Tooltip } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import dayjs from 'dayjs'
import { useState } from 'react'
import type { Activity, ActivityApplication, VolunteerProfile } from '../api/types'
import { applicationStatusColor } from '../activity/labels'
import { ErrorAlert } from '../common/ErrorAlert'
import { fullName, rowActionsOf, sortApplications } from './applications'
import { useActivityApplications, useDecideApplication, type Decision } from './queries'
import { VolunteerProfileModal } from './VolunteerProfileModal'

// Q99 … Q101: the applications and invitations of one activity, with accept and reject.
export function ApplicationsTable({ activity, now }: { activity: Activity; now: Date }) {
  const applications = useActivityApplications(activity.id)
  const decide = useDecideApplication(activity.id)
  const [shown, setShown] = useState<VolunteerProfile | null>(null)
  const [rejecting, setRejecting] = useState<ActivityApplication | null>(null)

  const run = (application: ActivityApplication, decision: Decision) =>
    decide.mutate(
      { applicationId: application.id, decision },
      {
        onSuccess: () => {
          setRejecting(null)
          notifications.show({
            color: 'sage',
            message: `${fullName(application.volunteer)}: ${decision === 'accept' ? 'accepted' : 'rejected'}`,
          })
        },
        onError: (e) => {
          setRejecting(null)
          notifications.show({ color: 'wine', title: 'Could not decide', message: e.message })
        },
      },
    )

  if (applications.isPending) {
    return (
      <Center mt="md">
        <Loader />
      </Center>
    )
  }
  if (applications.isError) {
    return <ErrorAlert error={applications.error} />
  }
  if (applications.data.length === 0) {
    return <Text c="dimmed">No applications or invitations yet.</Text>
  }

  return (
    <>
      <Table striped>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Volunteer</Table.Th>
            <Table.Th>Since</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th />
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {sortApplications(applications.data).map((application) => {
            const actions = rowActionsOf(application, activity, now)
            return (
              <Table.Tr key={application.id} data-testid="application-row">
                <Table.Td>
                  <Anchor component="button" type="button" onClick={() => setShown(application.volunteer)}>
                    {fullName(application.volunteer)}
                  </Anchor>
                  {application.volunteer.city && (
                    <Text size="xs" c="dimmed">
                      {application.volunteer.city}
                    </Text>
                  )}
                  {/* Q7d: the contact comes with the acceptance. */}
                  {application.volunteerEmail && <Text size="xs">{application.volunteerEmail}</Text>}
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{dayjs(application.appliedAt).format('D MMM, HH:mm')}</Text>
                  <Text size="xs" c="dimmed">
                    {application.origin === 'INVITATION' ? 'Invitation' : 'Application'}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <Badge color={applicationStatusColor[application.status]} variant="light">
                    {application.status}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  {actions.kind === 'waiting' && (
                    <Text size="xs" c="dimmed">
                      Waiting for the volunteer
                    </Text>
                  )}
                  {actions.kind === 'decide' && (
                    <Group gap="xs" wrap="nowrap">
                      <Tooltip label={actions.acceptBlocked} disabled={!actions.acceptBlocked}>
                        {/* A disabled button gets no mouse events; the wrapper keeps the tooltip working. */}
                        <span>
                          <Button
                            size="xs"
                            disabled={actions.acceptBlocked !== null}
                            loading={decide.isPending && decide.variables?.applicationId === application.id}
                            onClick={() => run(application, 'accept')}
                          >
                            Accept
                          </Button>
                        </span>
                      </Tooltip>
                      <Button size="xs" color="wine" variant="light" onClick={() => setRejecting(application)}>
                        Reject
                      </Button>
                    </Group>
                  )}
                </Table.Td>
              </Table.Tr>
            )
          })}
        </Table.Tbody>
      </Table>

      <VolunteerProfileModal volunteer={shown} onClose={() => setShown(null)} />

      {/* Q101: a rejection is final (one application per volunteer and activity, Q5b) and emails the volunteer. */}
      <Modal opened={rejecting !== null} onClose={() => setRejecting(null)} title="Reject this application?">
        {rejecting && (
          <Stack>
            <Text size="sm">
              {fullName(rejecting.volunteer)} cannot apply to this activity again and gets an email.
            </Text>
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setRejecting(null)}>
                Back
              </Button>
              <Button color="wine" loading={decide.isPending} onClick={() => run(rejecting, 'reject')}>
                Reject
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
    </>
  )
}
