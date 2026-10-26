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

export type Activity = Schemas['ActivityResponseDto']
export type ActivityPage = Schemas['ActivityPageResponseDto']
export type Priority = Activity['priority']
export type ActivityStatus = Activity['status']
export type OrganizationProfile = Schemas['OrganizationProfileResponseDto']

export type Score = Schemas['ScoreDto']
export type RecommendedActivity = Schemas['RecommendedActivityDto']

export type MyApplication = Schemas['MyApplicationResponseDto']
export type ApplicationStatus = MyApplication['status']

export type OwnOrganizationProfile = Schemas['OwnOrganizationProfileResponseDto']
export type OrganizationProfileUpdate = Schemas['OrganizationUpdateProfileRequestDto']
export type ActivityRequest = Schemas['ActivityRequestDto']

export type VolunteerProfile = Schemas['VolunteerProfileResponseDto']
export type ActivityApplication = Schemas['ActivityApplicationResponseDto']
export type RecommendedVolunteer = Schemas['RecommendedVolunteerDto']
export type HistoryItem = Schemas['HistoryItemResponseDto']

export type AdminUser = Schemas['AdminUserResponseDto']
export type AdminUserPage = Schemas['AdminUserPageResponseDto']
export type AuthorityRole = AdminUser['roles'][number]
