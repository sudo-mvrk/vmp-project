import type { OrganizationProfileUpdate, OwnOrganizationProfile } from '../api/types'
import type { GeoPoint } from '../map/geo'

export type Values = {
  organizationName: string
  description: string
  websiteUrl: string
  location: GeoPoint | null
  city: string
  fullAddress: string
  countryCode: string
}

export function initialValues(profile: OwnOrganizationProfile): Values {
  return {
    organizationName: profile.organizationName,
    description: profile.description ?? '',
    websiteUrl: profile.websiteUrl ?? '',
    location: profile.lat !== null && profile.lon !== null ? { lat: profile.lat, lon: profile.lon } : null,
    city: profile.city ?? '',
    fullAddress: profile.fullAddress ?? '',
    countryCode: profile.countryCode ?? '',
  }
}

// Partial update like the volunteer's (Q2b, Q87): text fields are always sent, so an emptied one is stored
// empty (an empty website passes the backend's @URL check); the location cannot be cleared (Q71); an empty
// country code is left out, because the backend would reject it.
export function toUpdateRequest(values: Values): OrganizationProfileUpdate {
  const countryCode = values.countryCode.trim().toUpperCase()
  return {
    organizationName: values.organizationName.trim(),
    description: values.description.trim(),
    websiteUrl: values.websiteUrl.trim(),
    lat: values.location?.lat,
    lon: values.location?.lon,
    city: values.city.trim(),
    fullAddress: values.fullAddress.trim(),
    countryCode: countryCode || undefined,
  }
}
