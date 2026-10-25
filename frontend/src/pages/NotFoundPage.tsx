import { Anchor, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <Stack>
      <Title order={2}>Page not found</Title>
      <Text>There is nothing at this address.</Text>
      <Anchor component={Link} to="/">
        Go to the start page
      </Anchor>
    </Stack>
  )
}
