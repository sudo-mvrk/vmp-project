import {
  Alert,
  Button,
  Group,
  MultiSelect,
  NumberInput,
  Select,
  Stack,
  Switch,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core'
import { DateInput, TimeInput } from '@mantine/dates'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import { ApiError } from '../api/client'
import type { ActivityRequest } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { useCertifications, useSkills } from '../dictionary/queries'
import { skillOptions } from '../dictionary/skills'
import { LocationPicker } from '../map/LocationPicker'
import { FORM_FIELDS, formErrorsOf, toRequest, validate, weightsSum, type Values } from './activityForm'

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

type Props = {
  initial: Values
  submitLabel: string
  // Q97: shown when an open activity with applicants is edited.
  openNote?: boolean
  onSubmit: (request: ActivityRequest, handlers: { onError: (e: Error) => void }) => void
  pending: boolean
  error: Error | null
}

// Create and edit share one form (Q89). A PUT replaces the whole activity (Q4c).
export function ActivityForm({ initial, submitLabel, openNote, onSubmit, pending, error }: Props) {
  const [today] = useState(() => new Date())
  const skills = useSkills()
  const certifications = useCertifications()
  const form = useForm<Values>({ initialValues: initial, validate })

  const submit = form.onSubmit((values) =>
    onSubmit(toRequest(values), {
      onError: (e) => e instanceof ApiError && form.setErrors(formErrorsOf(e.fieldErrors)),
    }),
  )
  const sum = weightsSum(form.values)

  return (
    <form onSubmit={submit}>
      <Stack>
        <ErrorAlert error={error ?? skills.error ?? certifications.error} shownFields={FORM_FIELDS} />
        {openNote && (
          <Alert color="amber">
            This activity is open. Volunteers who applied are not notified of changes.
          </Alert>
        )}
        <TextInput label="Title" withAsterisk maxLength={150} {...form.getInputProps('title')} />
        <TextInput
          label="Short description"
          description="Shown in lists."
          maxLength={500}
          {...form.getInputProps('shortDescription')}
        />
        <Textarea label="Full description" autosize minRows={3} {...form.getInputProps('fullDescription')} />

        <Group grow align="flex-start">
          <DateInput
            label="Date"
            withAsterisk
            valueFormat="ddd D MMM YYYY"
            minDate={today}
            {...form.getInputProps('date')}
          />
          <TimeInput label="Start" withAsterisk {...form.getInputProps('startTime')} />
          <TimeInput label="End" withAsterisk {...form.getInputProps('endTime')} />
        </Group>
        <Text size="xs" c="dimmed" mt={-8}>
          An activity starts and ends on the same day; the latest end is 23:59.
        </Text>

        <Group grow align="flex-start">
          <Select label="Priority" data={PRIORITIES} allowDeselect={false} {...form.getInputProps('priority')} />
          <NumberInput
            label="Volunteers needed"
            withAsterisk
            min={1}
            max={1000}
            allowDecimal={false}
            {...form.getInputProps('spotsNeeded')}
          />
        </Group>

        <MultiSelect
          label="Required skills"
          description="Volunteers with more of them rank higher."
          data={skillOptions(skills.data ?? [])}
          searchable
          clearable
          disabled={skills.isPending}
          {...form.getInputProps('skillIds')}
        />
        <MultiSelect
          label="Required certifications"
          description="Only volunteers holding all of them, valid on the date, can take part."
          data={(certifications.data ?? []).map((c) => ({ value: String(c.id), label: c.name }))}
          searchable
          clearable
          disabled={certifications.isPending}
          {...form.getInputProps('certificationIds')}
        />

        <LocationPicker
          value={form.values.location}
          onChange={(point) => form.setFieldValue('location', point)}
          error={form.errors.location as string | undefined}
        />
        <Group grow>
          <TextInput label="Address" {...form.getInputProps('fullAddress')} />
          <TextInput label="City" {...form.getInputProps('city')} />
        </Group>

        <Switch
          label="Custom weights for recommended volunteers"
          description="Used only to rank volunteers for this activity; otherwise the platform's weights apply."
          {...form.getInputProps('customWeights', { type: 'checkbox' })}
        />
        {form.values.customWeights && (
          <Stack gap={4}>
            <Group grow align="flex-start">
              <NumberInput label="Distance (α)" min={0} max={1} step={0.05} decimalScale={2} {...form.getInputProps('alpha')} />
              <NumberInput label="Skills (β)" min={0} max={1} step={0.05} decimalScale={2} {...form.getInputProps('beta')} />
              <NumberInput label="Priority (γ)" min={0} max={1} step={0.05} decimalScale={2} {...form.getInputProps('gamma')} />
            </Group>
            <Text size="sm" c={Math.abs(sum - 1) < 1e-9 ? 'dimmed' : 'wine'} data-testid="weights-sum">
              Sum: {sum.toFixed(2)} (must be 1.00)
            </Text>
          </Stack>
        )}

        <Group justify="flex-end">
          <Button type="submit" loading={pending}>
            {submitLabel}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
