import { Anchor, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'

export function NoAccessPage() {
  return (
    <Stack>
      <Title order={2}>No access</Title>
      <Text>This page belongs to another kind of account.</Text>
      <Anchor component={Link} to="/">
        Go to the start page
      </Anchor>
    </Stack>
  )
}
