import { Anchor, Stack, Text, Title } from '@mantine/core'
import { Link, Navigate } from 'react-router'
import { homePathOf, roleOf, useCurrentUser } from '../auth/session'

export function HomePage() {
  const { data: user } = useCurrentUser()
  const role = user ? roleOf(user) : null

  if (role === 'VOLUNTEER' || role === 'ORGANIZATION') {
    return <Navigate to={homePathOf(role)} replace />
  }
  if (role === 'ADMIN') {
    return (
      <Stack>
        <Title order={2}>Administrator</Title>
        <Text>
          There are no admin screens in this version. Use the API through{' '}
          <Anchor href="/swagger-ui/index.html">Swagger UI</Anchor> on the backend.
        </Text>
      </Stack>
    )
  }
  return (
    <Stack>
      <Title order={2}>Volunteering, matched</Title>
      <Text>
        VolunNear recommends activities to volunteers by distance, skills and priority, and shows organizations the
        volunteers who fit best.
      </Text>
      <Text>
        <Anchor component={Link} to="/activities">
          Browse activities
        </Anchor>{' '}
        or{' '}
        <Anchor component={Link} to="/register">
          create an account
        </Anchor>
        .
      </Text>
    </Stack>
  )
}
