import { Anchor, Badge, Button, Text } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { Link, useLocation } from 'react-router'
import { applicationStatusColor } from './labels'
import type { ApplyState } from './apply'
import { useApply } from './queries'

// Q78, Q79: the apply control for one state; a refusal of the backend (409) becomes a notification.
export function ApplyButton({ activityId, state }: { activityId: number; state: ApplyState }) {
  const apply = useApply()
  const location = useLocation()

  switch (state.kind) {
    case 'hidden':
      return null
    case 'login':
      return (
        <Anchor component={Link} to={`/login?next=${encodeURIComponent(location.pathname)}`} size="sm">
          Log in to apply
        </Anchor>
      )
    case 'applied':
      // Q115: the actions on an application live in one place, the volunteer's applications page.
      return (
        <>
          <Badge color={applicationStatusColor[state.status]} variant="light">
            Application {state.status.toLowerCase()}
          </Badge>
          <Anchor component={Link} to="/volunteer/applications" size="sm">
            Manage in My applications
          </Anchor>
        </>
      )
    case 'closed':
      return (
        <Text size="sm" c="dimmed">
          Not open for applications
        </Text>
      )
    case 'full':
      return (
        <Button size="xs" disabled>
          No free spots
        </Button>
      )
    case 'apply':
      return (
        <Button
          size="xs"
          loading={apply.isPending}
          onClick={() =>
            apply.mutate(activityId, {
              onSuccess: () => notifications.show({ color: 'sage', message: 'Applied. The organization will decide.' }),
              onError: (e) => notifications.show({ color: 'wine', title: 'Could not apply', message: e.message }),
            })
          }
        >
          Apply
        </Button>
      )
  }
}
