import { Button, Container, Group, List, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import {
  IconArrowRight,
  IconBuildingCommunity,
  IconCalendarCheck,
  IconCheck,
  IconHeartHandshake,
  IconListDetails,
  IconRoute,
} from '@tabler/icons-react'
import { Link, Navigate } from 'react-router'
import { homePathOf, roleOf, useCurrentUser } from '../auth/session'
import dark from '../common/dark.module.css'
import classes from './HomePage.module.css'

// Q127: an illustration of what a recommendation shows, not a score of real data.
const EXAMPLE = [
  { label: 'Distance', weight: 'α 0.40', value: 0.79 },
  { label: 'Skills', weight: 'β 0.40', value: 1.0 },
  { label: 'Priority', weight: 'γ 0.20', value: 0.67 },
]

const STEPS = [
  {
    icon: IconListDetails,
    title: 'Organizations publish activities',
    text: 'Place, time, the skills and certifications it needs, how many volunteers and how urgent it is.',
  },
  {
    icon: IconRoute,
    title: 'Smart Distribution ranks the matches',
    text: 'Only volunteers who are free at that time, certified and with capacity left are considered; they are ranked by distance, skills and priority.',
  },
  {
    icon: IconCalendarCheck,
    title: 'Apply, invite, take part',
    text: 'Volunteers apply or accept invitations, organizations decide, and completed activities stay in the history.',
  },
]

// The landing page (D2): what the platform is and how matching works. Signed-in users go to their own start page.
export function HomePage() {
  const { data: user } = useCurrentUser()
  const role = user ? roleOf(user) : null
  if (role) {
    return <Navigate to={homePathOf(role)} replace />
  }

  return (
    <div className={dark.page}>
      <Container size="lg" className={classes.hero}>
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing={56} style={{ alignItems: 'center' }}>
          <Stack gap="xl">
            <Text className={dark.eyebrow}>Volunteering Management Platform</Text>
            <Title order={1} className={classes.title}>
              The right volunteer for every task.
            </Title>
            <Text className={`${dark.muted} ${classes.lead}`}>
              VMP brings organizations and volunteers together. It recommends activities to volunteers and volunteers
              to organizations, and always shows why one match ranks above another.
            </Text>
            <Group gap="sm">
              <Button
                component={Link}
                to="/activities"
                variant="white"
                color="navy"
                size="md"
                rightSection={
                  <span className={classes.arrow}>
                    <IconArrowRight size={14} stroke={2} />
                  </span>
                }
              >
                Browse activities
              </Button>
              <Button component={Link} to="/register" variant="outline" color="gray.0" size="md">
                Create an account
              </Button>
            </Group>
          </Stack>

          <div className={`${dark.glass} ${classes.scoreCard}`} aria-label="Example of a score breakdown">
            <Group justify="space-between" mb="lg">
              <div>
                <Text className={dark.eyebrow}>Example match</Text>
                <Text fw={600} mt={4}>
                  Sort food donations
                </Text>
              </div>
              <Text className={dark.muted} size="sm">
                Thu 09:00
              </Text>
            </Group>
            <Stack gap="md">
              {EXAMPLE.map((row) => (
                <div key={row.label}>
                  <Group justify="space-between" mb={6}>
                    <Text size="sm" className={dark.muted}>
                      {row.label} <span style={{ opacity: 0.6 }}>({row.weight})</span>
                    </Text>
                    <Text size="sm" fw={600}>
                      {row.value.toFixed(2)}
                    </Text>
                  </Group>
                  <div className={classes.bar}>
                    <div className={classes.barFill} style={{ width: `${row.value * 100}%` }} />
                  </div>
                </div>
              ))}
            </Stack>
            <Group justify="space-between" mt="xl" pt="md" style={{ borderTop: '1px solid rgba(255,255,255,0.12)' }}>
              <Text fw={600}>Total</Text>
              <Text fw={700} size="xl">
                0.85
              </Text>
            </Group>
          </div>
        </SimpleGrid>
      </Container>

      <Container size="lg" className={classes.section}>
        <Stack gap={8} mb={40}>
          <Text className={dark.eyebrow}>How it works</Text>
          <Title order={2} className={classes.sectionTitle}>
            From a published activity to a completed one
          </Title>
        </Stack>
        <SimpleGrid cols={{ base: 1, md: 3 }} spacing="lg">
          {STEPS.map((step, index) => (
            <div key={step.title} className={`${dark.glass} ${classes.card}`}>
              <Group justify="space-between" mb="lg">
                <span className={dark.ring}>
                  <step.icon size={22} stroke={1.3} />
                </span>
                <Text className={dark.muted} ff="'Cormorant Garamond', serif" fz={28}>
                  0{index + 1}
                </Text>
              </Group>
              <Text fw={600} mb={8}>
                {step.title}
              </Text>
              <Text size="sm" className={dark.muted} lh={1.7}>
                {step.text}
              </Text>
            </div>
          ))}
        </SimpleGrid>
      </Container>

      <Container size="lg" className={classes.section}>
        <div className={dark.glass} style={{ padding: 'clamp(28px, 5vw, 56px)' }}>
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing={48} style={{ alignItems: 'center' }}>
            <Stack gap="md">
              <Text className={dark.eyebrow}>Smart Distribution</Text>
              <Title order={2} className={classes.sectionTitle}>
                Transparent by design
              </Title>
              <Text className={dark.muted} lh={1.8}>
                Every match gets three scores between 0 and 1: how close the place is, how well the skills fit and how
                urgent the activity is. Their weighted sum ranks the list, and the weights are always shown next to the
                result. Nobody has to trust a black box.
              </Text>
            </Stack>
            <Stack gap="sm" align="center">
              <Text className={classes.formula}>
                <span className={classes.term}>score =</span> <span className={classes.term}>α·geo +</span>{' '}
                <span className={classes.term}>β·skill +</span> <span className={classes.term}>γ·priority</span>
              </Text>
              <Text size="sm" className={dark.muted}>
                α + β + γ = 1 · every component in [0, 1]
              </Text>
            </Stack>
          </SimpleGrid>
        </div>
      </Container>

      <Container size="lg" className={classes.section}>
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg">
          <Audience
            icon={<IconHeartHandshake size={22} stroke={1.3} />}
            title="For volunteers"
            points={[
              'A profile with your place, skills, certifications and free time',
              'Recommendations that fit your week, with the reason for each',
              'Applications, invitations and your history in one place',
            ]}
          />
          <Audience
            icon={<IconBuildingCommunity size={22} stroke={1.3} />}
            title="For organizations"
            points={[
              'Activities with the skills, certifications and spots they need',
              'The volunteers who fit best, ranked and explained, ready to invite',
              'Applications to accept or reject, and a clear count of free spots',
            ]}
          />
        </SimpleGrid>
        <Group justify="center" mt={56}>
          <Button component={Link} to="/register" variant="white" color="navy" size="md">
            Join the platform
          </Button>
          <Button component={Link} to="/login" variant="subtle" color="gray.0" size="md">
            Log in
          </Button>
        </Group>
      </Container>

      <footer className={classes.footer}>
        <Container size="lg">
          <Text size="xs" className={dark.muted}>
            VMP · Volunteering Management Platform
          </Text>
        </Container>
      </footer>
    </div>
  )
}

function Audience({ icon, title, points }: { icon: React.ReactNode; title: string; points: string[] }) {
  return (
    <div className={`${dark.glass} ${classes.card}`}>
      <Group gap="md" mb="lg">
        <span className={dark.ring}>{icon}</span>
        <Text fw={600} size="lg">
          {title}
        </Text>
      </Group>
      <List spacing="sm" className={dark.muted} icon={<IconCheck size={16} stroke={1.5} color="var(--mantine-color-navy-2)" />}>
        {points.map((point) => (
          <List.Item key={point}>{point}</List.Item>
        ))}
      </List>
    </div>
  )
}
