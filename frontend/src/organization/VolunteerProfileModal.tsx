import { Badge, Group, List, Modal, Stack, Text } from '@mantine/core'
import type { VolunteerProfile } from '../api/types'
import { dayLabel, DAYS } from '../volunteer/availability'
import { fullName } from './applications'

// Q102, Q107: what an organization may see of a volunteer (no account data, Q3a; no date of birth).
export function VolunteerProfileModal({ volunteer, onClose }: { volunteer: VolunteerProfile | null; onClose: () => void }) {
  return (
    <Modal opened={volunteer !== null} onClose={onClose} title={volunteer && fullName(volunteer)} size="lg">
      {volunteer && (
        <Stack gap="sm">
          {(volunteer.city || volunteer.region) && (
            <Text size="sm" c="dimmed">
              {[volunteer.city, volunteer.region].filter(Boolean).join(', ')}
            </Text>
          )}
          {volunteer.bio && <Text style={{ whiteSpace: 'pre-line' }}>{volunteer.bio}</Text>}
          <div>
            <Text fw={600} size="sm">
              Skills
            </Text>
            {volunteer.skills.length === 0 ? (
              <Text size="sm" c="dimmed">
                None
              </Text>
            ) : (
              <Group gap={4}>
                {volunteer.skills.map((skill) => (
                  <Badge key={skill.id} variant="outline" size="sm">
                    {skill.skillName}
                  </Badge>
                ))}
              </Group>
            )}
          </div>
          <div>
            <Text fw={600} size="sm">
              Certifications
            </Text>
            {volunteer.certifications.length === 0 ? (
              <Text size="sm" c="dimmed">
                None
              </Text>
            ) : (
              <List size="sm">
                {volunteer.certifications.map((certification) => (
                  <List.Item key={certification.certificationId}>
                    {certification.name}
                    {certification.validUntil ? ` (valid until ${certification.validUntil})` : ''}
                  </List.Item>
                ))}
              </List>
            )}
          </div>
          <div>
            <Text fw={600} size="sm">
              Availability
            </Text>
            {volunteer.availability.length === 0 ? (
              <Text size="sm" c="dimmed">
                None
              </Text>
            ) : (
              <List size="sm">
                {[...volunteer.availability]
                  .sort((a, b) => DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek) || a.start.localeCompare(b.start))
                  .map((slot) => (
                    <List.Item key={`${slot.dayOfWeek}-${slot.start}`}>
                      {dayLabel(slot.dayOfWeek).slice(0, 3)} {slot.start.slice(0, 5)}–{slot.end.slice(0, 5)}
                    </List.Item>
                  ))}
              </List>
            )}
          </div>
        </Stack>
      )}
    </Modal>
  )
}
