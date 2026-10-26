import { Container, List, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { IconCheck } from '@tabler/icons-react'
import type { ReactNode } from 'react'
import classes from './dark.module.css'

// Q126: login and registration on the deep navy page, a short word about the platform beside the form.
export function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className={classes.page}>
      <Container size="lg" py={{ base: 40, md: 80 }}>
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing={64} style={{ alignItems: 'center' }}>
          <Stack gap="lg" visibleFrom="md">
            <Text className={classes.eyebrow}>Volunteering Management Platform</Text>
            <Title order={1} c="white">
              {title}
            </Title>
            <List
              spacing="sm"
              className={classes.muted}
              icon={<IconCheck size={18} stroke={1.5} color="var(--mantine-color-navy-2)" />}
            >
              <List.Item>Activities near you that fit your skills and your week</List.Item>
              <List.Item>Every recommendation shows why: distance, skills, priority</List.Item>
              <List.Item>Organizations find and invite the volunteers who fit best</List.Item>
            </List>
          </Stack>
          <div className={classes.lightCard}>{children}</div>
        </SimpleGrid>
      </Container>
    </div>
  )
}
