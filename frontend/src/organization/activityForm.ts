import type { Activity, ActivityRequest, OwnOrganizationProfile, Priority } from '../api/types'
import type { GeoPoint } from '../map/geo'

export type Values = {
  title: string
  shortDescription: string
  fullDescription: string
  priority: Priority
  location: GeoPoint | null
  city: string
  fullAddress: string
  // Q91: one day and two times, so an activity cannot span days (Q13).
  date: string | null
  startTime: string
  endTime: string
  spotsNeeded: number | string
  skillIds: string[]
  certificationIds: string[]
  customWeights: boolean
  alpha: number | string
  beta: number | string
  gamma: number | string
}

// The global defaults of application.yml (Q12), only as a starting point when the override is switched on.
const DEFAULT_WEIGHTS = { alpha: 0.4, beta: 0.4, gamma: 0.2 }

// Q94: a new activity starts at the organization's place, if it has one.
export function newValues(organization: OwnOrganizationProfile | undefined): Values {
  const location =
    organization && organization.lat !== null && organization.lon !== null
      ? { lat: organization.lat, lon: organization.lon }
      : null
  return {
    title: '',
    shortDescription: '',
    fullDescription: '',
    priority: 'MEDIUM',
    location,
    city: organization?.city ?? '',
    fullAddress: organization?.fullAddress ?? '',
    date: null,
    startTime: '',
    endTime: '',
    spotsNeeded: 1,
    skillIds: [],
    certificationIds: [],
    customWeights: false,
    ...DEFAULT_WEIGHTS,
  }
}

export function valuesOf(activity: Activity): Values {
  return {
    title: activity.title,
    shortDescription: activity.shortDescription ?? '',
    fullDescription: activity.fullDescription ?? '',
    priority: activity.priority,
    location: { lat: activity.lat, lon: activity.lon },
    city: activity.city ?? '',
    fullAddress: activity.fullAddress ?? '',
    date: activity.startsAt.slice(0, 10),
    startTime: activity.startsAt.slice(11, 16),
    endTime: activity.endsAt.slice(11, 16),
    spotsNeeded: activity.spotsNeeded,
    skillIds: activity.requiredSkills.map((skill) => String(skill.id)),
    certificationIds: activity.requiredCertifications.map((certification) => String(certification.id)),
    customWeights: activity.weights !== null,
    ...(activity.weights ?? DEFAULT_WEIGHTS),
  }
}

const asNumber = (value: number | string) => (typeof value === 'number' ? value : 0)

export function weightsSum(values: Pick<Values, 'alpha' | 'beta' | 'gamma'>): number {
  return asNumber(values.alpha) + asNumber(values.beta) + asNumber(values.gamma)
}

// The backend's tolerance (ScoringWeights.SUM_TOLERANCE).
const SUM_TOLERANCE = 1e-9

// Client checks only what a person can get wrong while typing (Q91, Q92); the backend checks everything
// again, including a start in the past, and its message is shown.
export function validate(values: Values): Partial<Record<keyof Values, string>> {
  const errors: Partial<Record<keyof Values, string>> = {}
  if (!values.title.trim()) errors.title = 'Title is required'
  if (!values.location) errors.location = 'Pick the place on the map'
  if (!values.date) errors.date = 'Date is required'
  if (!values.startTime) errors.startTime = 'Start is required'
  if (!values.endTime) errors.endTime = 'End is required'
  if (values.startTime && values.endTime && values.endTime <= values.startTime) {
    errors.endTime = 'End must be after the start'
  }
  if (typeof values.spotsNeeded !== 'number' || values.spotsNeeded < 1 || values.spotsNeeded > 1000) {
    errors.spotsNeeded = 'Between 1 and 1000'
  }
  if (values.customWeights && Math.abs(weightsSum(values) - 1) > SUM_TOLERANCE) {
    errors.gamma = 'α + β + γ must be 1'
  }
  return errors
}

// Q4c: a PUT replaces the whole activity, so every field is sent; Q93: the priority always explicitly.
export function toRequest(values: Values): ActivityRequest {
  const location = values.location
  if (!location) {
    throw new Error('validate() first: an activity needs a location (Q4b)')
  }
  return {
    title: values.title.trim(),
    shortDescription: values.shortDescription.trim(),
    fullDescription: values.fullDescription.trim(),
    priority: values.priority,
    lat: location.lat,
    lon: location.lon,
    city: values.city.trim(),
    fullAddress: values.fullAddress.trim(),
    startsAt: `${values.date}T${values.startTime}`,
    endsAt: `${values.date}T${values.endTime}`,
    spotsNeeded: asNumber(values.spotsNeeded),
    requiredSkillIds: values.skillIds.map(Number),
    requiredCertificationIds: values.certificationIds.map(Number),
    weights: values.customWeights
      ? { alpha: asNumber(values.alpha), beta: asNumber(values.beta), gamma: asNumber(values.gamma) }
      : undefined,
  }
}

// Backend field names → the inputs of the form; anything else stays in the alert above the form.
const FIELD_OF: Record<string, keyof Values> = {
  title: 'title',
  shortDescription: 'shortDescription',
  lat: 'location',
  lon: 'location',
  city: 'city',
  fullAddress: 'fullAddress',
  startsAt: 'startTime',
  endsAt: 'endTime',
  spotsNeeded: 'spotsNeeded',
  'weights.alpha': 'alpha',
  'weights.beta': 'beta',
  'weights.gamma': 'gamma',
}

export const FORM_FIELDS = Object.keys(FIELD_OF)

export function formErrorsOf(fieldErrors: Record<string, string>): Partial<Record<keyof Values, string>> {
  const errors: Partial<Record<keyof Values, string>> = {}
  for (const [field, message] of Object.entries(fieldErrors)) {
    const input = FIELD_OF[field]
    if (input) errors[input] = message
  }
  return errors
}
