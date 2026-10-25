import type { OwnVolunteerProfile, VolunteerProfileUpdate } from '../api/types'
import type { GeoPoint } from '../map/geo'

export type Values = {
  firstName: string
  lastName: string
  dateOfBirth: string | null
  bio: string
  location: GeoPoint | null
  locationName: string
  city: string
  region: string
  countryCode: string
  radius: number | string
  maxActiveAssignments: number | string
}

export function initialValues(profile: OwnVolunteerProfile): Values {
  return {
    firstName: profile.firstName,
    lastName: profile.lastName,
    dateOfBirth: profile.dateOfBirth,
    bio: profile.bio ?? '',
    location: profile.lat !== null && profile.lon !== null ? { lat: profile.lat, lon: profile.lon } : null,
    locationName: profile.locationName ?? '',
    city: profile.city ?? '',
    region: profile.region ?? '',
    countryCode: profile.countryCode ?? '',
    radius: profile.radius ?? '',
    maxActiveAssignments: profile.maxActiveAssignments,
  }
}

// The update is partial: an absent field keeps the stored value (Q2b). Text fields are always sent, so an
// emptied field is stored empty. The location cannot be cleared (Q71), and an empty country code or radius
// is left out, because the backend would reject it.
export function toUpdateRequest(values: Values): VolunteerProfileUpdate {
  const countryCode = values.countryCode.trim().toUpperCase()
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    dateOfBirth: values.dateOfBirth ?? undefined,
    bio: values.bio.trim(),
    lat: values.location?.lat,
    lon: values.location?.lon,
    locationName: values.locationName.trim(),
    city: values.city.trim(),
    region: values.region.trim(),
    countryCode: countryCode || undefined,
    radius: typeof values.radius === 'number' ? values.radius : undefined,
    maxActiveAssignments: typeof values.maxActiveAssignments === 'number' ? values.maxActiveAssignments : undefined,
  }
}
