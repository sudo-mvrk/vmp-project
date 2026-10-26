import { Button, Center, Group, Loader, Paper, Stack, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { ApiError } from '../api/client'
import type { OwnOrganizationProfile } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { LocationPicker } from '../map/LocationPicker'
import { PageHeader } from '../common/PageHeader'
import { initialValues, toUpdateRequest, type Values } from './organizationProfile'
import { useOwnOrganization, useUpdateOrganization } from './queries'

const FIELDS = ['organizationName', 'description', 'websiteUrl', 'city', 'fullAddress', 'countryCode']

export function ProfilePage() {
  const profile = useOwnOrganization()

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
  return (
    <Stack maw={880} mx="auto" w="100%" gap="lg">
      <PageHeader eyebrow="Organization profile" title={profile.data.organizationName} description={profile.data.username} />
      <Paper withBorder p="xl">
        <OrganizationProfileForm profile={profile.data} />
      </Paper>
    </Stack>
  )
}

// Q87: one form, saved with one PUT.
export function OrganizationProfileForm({ profile }: { profile: OwnOrganizationProfile }) {
  const update = useUpdateOrganization()
  const form = useForm<Values>({
    initialValues: initialValues(profile),
    validate: {
      organizationName: (v) => (v.trim() ? null : 'Name is required'),
      countryCode: (v) => (!v.trim() || /^[A-Za-z]{2}$/.test(v.trim()) ? null : 'Two letters (ISO 3166-1), e.g. CZ'),
    },
  })

  const submit = form.onSubmit((values) =>
    update.mutate(toUpdateRequest(values), {
      onSuccess: (saved) => {
        form.setValues(initialValues(saved))
        form.resetDirty(initialValues(saved))
        notifications.show({ color: 'sage', message: 'Profile saved' })
      },
      onError: (e) => e instanceof ApiError && form.setErrors(e.fieldErrors),
    }),
  )

  return (
    <form onSubmit={submit}>
      <Stack>
        <ErrorAlert error={update.error} shownFields={FIELDS} />
        <TextInput label="Organization name" withAsterisk {...form.getInputProps('organizationName')} />
        <Textarea label="About the organization" autosize minRows={3} {...form.getInputProps('description')} />
        <TextInput label="Website" placeholder="https://" {...form.getInputProps('websiteUrl')} />
        <LocationPicker
          value={form.values.location}
          onChange={(point) => form.setFieldValue('location', point)}
          error={update.error instanceof ApiError ? update.error.fieldErrors.location : undefined}
        />
        <Group grow>
          <TextInput label="Address" {...form.getInputProps('fullAddress')} />
          <TextInput label="City" {...form.getInputProps('city')} />
          <TextInput label="Country code" placeholder="CZ" maxLength={2} {...form.getInputProps('countryCode')} />
        </Group>
        <Group justify="flex-end">
          <Button type="submit" loading={update.isPending}>
            Save
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
