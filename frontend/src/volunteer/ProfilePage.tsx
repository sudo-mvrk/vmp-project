import { Center, Loader, Paper, Stack, Tabs } from '@mantine/core'
import { ErrorAlert } from '../common/ErrorAlert'
import { PageHeader } from '../common/PageHeader'
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
    <Stack maw={880} mx="auto" w="100%" gap="lg">
      <PageHeader
        eyebrow="Volunteer profile"
        title={`${data.firstName} ${data.lastName}`}
        description={`${data.username} · what you set here decides which activities are recommended to you`}
      />
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
