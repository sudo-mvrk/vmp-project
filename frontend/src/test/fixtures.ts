import type { Activity, AppUser, MyApplication, Score } from '../api/types'

export const volunteerUser: AppUser = { id: 1, username: 'vol', email: 'vol@example.org', roles: ['ROLE_VOLUNTEER'] }
export const organizationUser: AppUser = { id: 2, username: 'org', email: 'org@example.org', roles: ['ROLE_ORGANIZATION'] }

export function activity(overrides: Partial<Activity> = {}): Activity {
  return {
    id: 7,
    organizationId: 3,
    title: 'Sort food donations',
    shortDescription: 'Sort and pack food',
    fullDescription: null,
    priority: 'HIGH',
    status: 'OPEN',
    lat: 50.08,
    lon: 14.42,
    city: 'Prague',
    fullAddress: null,
    startsAt: '2026-11-07T09:00:00',
    endsAt: '2026-11-07T12:00:00',
    spotsNeeded: 4,
    acceptedCount: 1,
    requiredSkills: [],
    requiredCertifications: [],
    weights: null,
    ...overrides,
  }
}

export function score(overrides: Partial<Score> = {}): Score {
  return {
    geo: 0.79356,
    skill: 1,
    priority: 0,
    total: 0.71742,
    weights: { alpha: 0.4, beta: 0.4, gamma: 0.2, halfDistanceKm: 5 },
    ...overrides,
  }
}

export function myApplication(overrides: Partial<MyApplication> = {}): MyApplication {
  return {
    id: 11,
    status: 'PENDING',
    origin: 'APPLICATION',
    appliedAt: '2026-10-26T10:00:00',
    decidedAt: null,
    activityId: 7,
    activityTitle: 'Sort food donations',
    activityStatus: 'OPEN',
    activityStartsAt: '2026-11-07T09:00:00',
    activityEndsAt: '2026-11-07T12:00:00',
    ...overrides,
  }
}

export const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
