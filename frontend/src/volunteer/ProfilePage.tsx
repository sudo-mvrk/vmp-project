import { Center, Loader, Paper, Stack, Tabs, Text, Title } from '@mantine/core'
import { ErrorAlert } from '../common/ErrorAlert'
import { AvailabilityForm } from './AvailabilityForm'
import { BasicProfileForm } from './BasicProfileForm'
import { CertificationsForm } from './CertificationsForm'
import { useOwnVolunteerProfile } from './queries'
import { SkillsForm } from './SkillsForm'

// Q72: four sections, each saved on its own with its own PUT.
export function ProfilePage() {
  const profile = useOwnVolunteerProfile()

  if (profile.isPending) {
    return (
      <Center mt="xl">
        <Loader />
      </Center>
    )
  }
  if (profile.isError) {
    return <ErrorAlert error={profile.error} />
  }

  const data = profile.data
  return (
    <Stack maw={860}>
      <div>
        <Title order={2}>
          {data.firstName} {data.lastName}
        </Title>
        <Text c="dimmed">{data.username}</Text>
      </div>
      <Tabs defaultValue="basic" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="basic">Profile and location</Tabs.Tab>
          <Tabs.Tab value="skills">Skills</Tabs.Tab>
          <Tabs.Tab value="certifications">Certifications</Tabs.Tab>
          <Tabs.Tab value="availability">Availability</Tabs.Tab>
        </Tabs.List>
        <Paper withBorder p="md" mt="sm">
          <Tabs.Panel value="basic">
            <BasicProfileForm profile={data} />
          </Tabs.Panel>
          <Tabs.Panel value="skills">
            <SkillsForm profile={data} />
          </Tabs.Panel>
          <Tabs.Panel value="certifications">
            <CertificationsForm profile={data} />
          </Tabs.Panel>
          <Tabs.Panel value="availability">
            <AvailabilityForm profile={data} />
          </Tabs.Panel>
        </Paper>
      </Tabs>
    </Stack>
  )
}
