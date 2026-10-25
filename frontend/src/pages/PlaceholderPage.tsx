import { Stack, Text, Title } from '@mantine/core'

// Stands in for the pages of the later frontend slices (F1 … F5).
export function PlaceholderPage({ title, slice }: { title: string; slice: string }) {
  return (
    <Stack>
      <Title order={2}>{title}</Title>
      <Text c="dimmed">Comes with slice {slice}.</Text>
    </Stack>
  )
}
