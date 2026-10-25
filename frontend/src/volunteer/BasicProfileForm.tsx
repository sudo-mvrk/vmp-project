import { Button, Group, NumberInput, Stack, Textarea, TextInput } from '@mantine/core'
import { DateInput } from '@mantine/dates'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { ApiError } from '../api/client'
import type { OwnVolunteerProfile } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { LocationPicker } from '../map/LocationPicker'
import { initialValues, toUpdateRequest, type Values } from './basicProfile'
import { useUpdateBasicProfile } from './queries'

const FIELDS = ['firstName', 'lastName', 'dateOfBirth', 'bio', 'locationName', 'city', 'region', 'countryCode', 'radius', 'maxActiveAssignments']

export function BasicProfileForm({ profile }: { profile: OwnVolunteerProfile }) {
  const [today] = useState(() => new Date())
  const update = useUpdateBasicProfile()
  const form = useForm<Values>({
    initialValues: initialValues(profile),
    validate: {
      firstName: (v) => (v.trim() ? null : 'First name is required'),
      lastName: (v) => (v.trim() ? null : 'Last name is required'),
      countryCode: (v) => (!v.trim() || /^[A-Za-z]{2}$/.test(v.trim()) ? null : 'Two letters (ISO 3166-1), e.g. CZ'),
      radius: (v) => (v === '' || (typeof v === 'number' && v >= 1 && v <= 500) ? null : 'Between 1 and 500 km'),
      maxActiveAssignments: (v) => (typeof v === 'number' && v >= 0 && v <= 10 ? null : 'Between 0 and 10'),
    },
  })

  const submit = form.onSubmit((values) =>
    update.mutate(toUpdateRequest(values), {
      onSuccess: (saved) => {
        form.setValues(initialValues(saved))
        form.resetDirty(initialValues(saved))
        notifications.show({ color: 'teal', message: 'Profile saved' })
      },
      onError: (e) => e instanceof ApiError && form.setErrors(e.fieldErrors),
    }),
  )

  const radius = typeof form.values.radius === 'number' ? form.values.radius : null

  return (
    <form onSubmit={submit}>
      <Stack>
        <ErrorAlert error={update.error} shownFields={FIELDS} />
        <Group grow>
          <TextInput label="First name" {...form.getInputProps('firstName')} />
          <TextInput label="Last name" {...form.getInputProps('lastName')} />
        </Group>
        <DateInput label="Date of birth" valueFormat="YYYY-MM-DD" maxDate={today} {...form.getInputProps('dateOfBirth')} />
        <Textarea label="About me" autosize minRows={2} {...form.getInputProps('bio')} />

        <LocationPicker
          value={form.values.location}
          onChange={(point) => form.setFieldValue('location', point)}
          radiusKm={radius}
          error={update.error instanceof ApiError ? update.error.fieldErrors.location : undefined}
        />
        <Group grow>
          <TextInput label="Place name" placeholder="e.g. Prague 3" {...form.getInputProps('locationName')} />
          <TextInput label="City" {...form.getInputProps('city')} />
        </Group>
        <Group grow>
          <TextInput label="Region" {...form.getInputProps('region')} />
          <TextInput label="Country code" placeholder="CZ" maxLength={2} {...form.getInputProps('countryCode')} />
        </Group>

        <Group grow align="flex-start">
          <NumberInput
            label="Search radius, km"
            description="Activities farther away are not recommended to you."
            min={1}
            max={500}
            allowDecimal={false}
            {...form.getInputProps('radius')}
          />
          <NumberInput
            label="Activities at the same time"
            description="How many accepted activities you can have open at once (0 pauses you)."
            min={0}
            max={10}
            allowDecimal={false}
            {...form.getInputProps('maxActiveAssignments')}
          />
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
