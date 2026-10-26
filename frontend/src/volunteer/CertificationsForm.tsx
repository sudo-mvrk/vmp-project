import { ActionIcon, Button, Group, Select, Stack, Text } from '@mantine/core'
import { DateInput } from '@mantine/dates'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import type { OwnVolunteerProfile } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { useCertifications } from '../dictionary/queries'
import { newKey, optionsForRow, toCertificationsRequest, type CertificationRow } from './certifications'
import { useReplaceCertifications } from './queries'

export function CertificationsForm({ profile }: { profile: OwnVolunteerProfile }) {
  const [today] = useState(() => new Date())
  const certifications = useCertifications()
  const replace = useReplaceCertifications()
  const [rows, setRows] = useState<CertificationRow[]>(() =>
    profile.certifications.map((c) => ({ key: newKey(), certificationId: String(c.certificationId), validUntil: c.validUntil })),
  )

  const change = (key: number, patch: Partial<CertificationRow>) =>
    setRows((current) => current.map((r) => (r.key === key ? { ...r, ...patch } : r)))
  const all = certifications.data ?? []

  const save = () =>
    replace.mutate(
      { certifications: toCertificationsRequest(rows) },
      { onSuccess: () => notifications.show({ color: 'sage', message: 'Certifications saved' }) },
    )

  return (
    <Stack>
      <ErrorAlert error={certifications.error ?? replace.error} />
      {rows.length === 0 && <Text c="dimmed">No certifications yet.</Text>}
      {rows.map((row) => (
        <Group key={row.key} align="flex-end" wrap="nowrap">
          <Select
            label="Certification"
            placeholder="Pick one"
            data={optionsForRow(all, rows, row)}
            value={row.certificationId}
            onChange={(value) => change(row.key, { certificationId: value })}
            searchable
            style={{ flex: 2 }}
          />
          <DateInput
            label="Valid until"
            placeholder="No expiry"
            valueFormat="YYYY-MM-DD"
            minDate={today}
            clearable
            value={row.validUntil}
            onChange={(value) => change(row.key, { validUntil: value })}
            style={{ flex: 1 }}
          />
          <ActionIcon
            variant="subtle"
            color="wine"
            size="lg"
            aria-label="Remove certification"
            onClick={() => setRows((current) => current.filter((r) => r.key !== row.key))}
          >
            ✕
          </ActionIcon>
        </Group>
      ))}
      <Group justify="space-between">
        <Button
          variant="default"
          onClick={() => setRows((current) => [...current, { key: newKey(), certificationId: null, validUntil: null }])}
          disabled={rows.length >= all.length}
        >
          Add certification
        </Button>
        <Button onClick={save} loading={replace.isPending} disabled={certifications.isPending}>
          Save
        </Button>
      </Group>
    </Stack>
  )
}
