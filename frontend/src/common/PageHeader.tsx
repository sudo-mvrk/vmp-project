import { Group, Stack, Text, Title } from '@mantine/core'
import type { ReactNode } from 'react'
import classes from './PageHeader.module.css'

type Props = {
  title: ReactNode
  // A small spaced caption above the title, like the landing page.
  eyebrow?: string
  description?: ReactNode
  // Next to the title but outside the heading, so its accessible name stays the title.
  badges?: ReactNode
  actions?: ReactNode
}

// D3: the same head on every working page: caption, serif title, one line of description, actions on the right.
export function PageHeader({ title, eyebrow, description, badges, actions }: Props) {
  return (
    <Group justify="space-between" align="flex-end" wrap="wrap" gap="md" className={classes.header}>
      <Stack gap={6}>
        {eyebrow && <Text className={classes.eyebrow}>{eyebrow}</Text>}
        <Group gap="sm" align="center">
          <Title order={2}>{title}</Title>
          {badges}
        </Group>
        {description && (
          <Text c="dimmed" size="sm" maw={640}>
            {description}
          </Text>
        )}
      </Stack>
      {actions && <Group gap="sm">{actions}</Group>}
    </Group>
  )
}
