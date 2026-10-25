import type { components } from './schema'

// Short names for the generated schemas (Q57). Regenerate schema.d.ts with `npm run gen:api`.
type Schemas = components['schemas']

export type AppUser = Schemas['AppUserResponseDto']
export type LoginRequest = Schemas['LoginRequestDto']
export type VolunteerRegistrationRequest = Schemas['VolunteerRegistrationRequestDto']
export type OrganizationRegistrationRequest = Schemas['OrganizationRegistrationRequestDto']

export type Skill = Schemas['SkillResponseDto']
export type Certification = Schemas['CertificationResponseDto']

export type OwnVolunteerProfile = Schemas['OwnVolunteerProfileResponseDto']
export type VolunteerProfileUpdate = Schemas['VolunteerUpdateProfileRequestDto']
export type AvailabilitySlot = Schemas['AvailabilitySlotDto']
export type DayOfWeek = AvailabilitySlot['dayOfWeek']
export type HeldCertificationRequest = Schemas['HeldCertificationRequestDto']
