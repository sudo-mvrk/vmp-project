import { Button, Group, MultiSelect, Stack } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import type { OwnVolunteerProfile } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { useSkills } from '../dictionary/queries'
import { useReplaceSkills } from './queries'
import { skillOptions } from './skills'

export function SkillsForm({ profile }: { profile: OwnVolunteerProfile }) {
  const skills = useSkills()
  const replace = useReplaceSkills()
  const [selected, setSelected] = useState(() => profile.skills.map((s) => String(s.id)))

  const save = () =>
    replace.mutate(
      { skillIds: selected.map(Number) },
      { onSuccess: () => notifications.show({ color: 'teal', message: 'Skills saved' }) },
    )

  return (
    <Stack>
      <ErrorAlert error={skills.error ?? replace.error} />
      <MultiSelect
        label="Skills"
        description="Matched against the skills an activity needs."
        placeholder={selected.length ? undefined : 'Pick your skills'}
        data={skillOptions(skills.data ?? [])}
        value={selected}
        onChange={setSelected}
        searchable
        clearable
        disabled={skills.isPending}
      />
      <Group justify="flex-end">
        <Button onClick={save} loading={replace.isPending} disabled={skills.isPending}>
          Save
        </Button>
      </Group>
    </Stack>
  )
}
