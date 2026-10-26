import { ActionIcon, Button, Group, Select, Stack, Text } from '@mantine/core'
import { TimeInput } from '@mantine/dates'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import type { DayOfWeek, OwnVolunteerProfile } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { useReplaceAvailability } from './queries'
import { DAYS, dayLabel, LATEST_END, newSlotKey, slotError, toAvailabilityRequest, toRows, type SlotRow } from './availability'

const dayOptions = DAYS.map((day) => ({ value: day, label: dayLabel(day) }))

export function AvailabilityForm({ profile }: { profile: OwnVolunteerProfile }) {
  const replace = useReplaceAvailability()
  const [rows, setRows] = useState<SlotRow[]>(() => toRows(profile.availability))
  const [showErrors, setShowErrors] = useState(false)

  const change = (key: number, patch: Partial<SlotRow>) =>
    setRows((current) => current.map((r) => (r.key === key ? { ...r, ...patch } : r)))

  const save = () => {
    if (rows.some((r) => slotError(r))) {
      setShowErrors(true)
      return
    }
    setShowErrors(false)
    replace.mutate(
      { slots: toAvailabilityRequest(rows) },
      {
        onSuccess: (saved) => {
          setRows(toRows(saved.availability))
          notifications.show({ color: 'sage', message: 'Availability saved' })
        },
      },
    )
  }

  return (
    <Stack>
      <Text size="sm" c="dimmed">
        Weekly times when you can help. An activity is recommended only if it fits into one of them.
      </Text>
      <ErrorAlert error={replace.error} />
      {rows.length === 0 && <Text c="dimmed">No times yet.</Text>}
      {rows.map((row) => {
        const error = showErrors ? slotError(row) : null
        return (
          <Stack key={row.key} gap={4}>
            <Group align="flex-end" wrap="nowrap">
              <Select
                label="Day"
                data={dayOptions}
                value={row.dayOfWeek}
                onChange={(value) => value && change(row.key, { dayOfWeek: value as DayOfWeek })}
                allowDeselect={false}
                style={{ flex: 1 }}
              />
              <TimeInput
                label="From"
                value={row.start}
                onChange={(e) => change(row.key, { start: e.currentTarget.value })}
                style={{ flex: 1 }}
              />
              <TimeInput
                label="To"
                maxTime={LATEST_END}
                value={row.end}
                onChange={(e) => change(row.key, { end: e.currentTarget.value })}
                style={{ flex: 1 }}
              />
              <ActionIcon
                variant="subtle"
                color="wine"
                size="lg"
                aria-label="Remove time"
                onClick={() => setRows((current) => current.filter((r) => r.key !== row.key))}
              >
                ✕
              </ActionIcon>
            </Group>
            {error && (
              <Text size="sm" c="wine">
                {error}
              </Text>
            )}
          </Stack>
        )
      })}
      <Group justify="space-between">
        <Button
          variant="default"
          onClick={() =>
            setRows((current) => [...current, { key: newSlotKey(), dayOfWeek: 'MONDAY', start: '09:00', end: '17:00' }])
          }
        >
          Add time
        </Button>
        <Button onClick={save} loading={replace.isPending}>
          Save
        </Button>
      </Group>
    </Stack>
  )
}
