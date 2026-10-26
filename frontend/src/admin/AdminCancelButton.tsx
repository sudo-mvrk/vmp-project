import { Button, Group, Modal, Stack, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { useAdminCancelActivity } from './queries'

// Q122: an admin removes an activity by cancelling it (Q31), after a confirmation.
export function AdminCancelButton({ activityId }: { activityId: number }) {
  const [confirming, setConfirming] = useState(false)
  const cancel = useAdminCancelActivity(activityId)

  const run = () =>
    cancel.mutate(undefined, {
      onSuccess: () => {
        setConfirming(false)
        notifications.show({ color: 'teal', message: 'Activity cancelled' })
      },
      onError: (e) => {
        setConfirming(false)
        notifications.show({ color: 'red', title: 'Not cancelled', message: e.message })
      },
    })

  return (
    <>
      <Button size="xs" color="red" variant="light" onClick={() => setConfirming(true)}>
        Cancel as admin
      </Button>
      <Modal opened={confirming} onClose={() => setConfirming(false)} title="Cancel this activity as admin?">
        <Stack>
          <Text size="sm">
            It cannot be reopened. Its volunteers and the organization are told by email.
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setConfirming(false)}>
              Back
            </Button>
            <Button color="red" loading={cancel.isPending} onClick={run}>
              Cancel activity
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  )
}
