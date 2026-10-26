import dayjs from 'dayjs'
import type { Activity } from '../api/types'

// Q86: the backend sends local times of its one configured zone (Q13) without an offset;
// dayjs reads such a string as it is, so the page shows the same clock time as the backend.
// An activity starts and ends on the same day (ActivityWindowRules), so the end is a time only.
export function formatWindow(startsAt: string, endsAt: string): string {
  return `${dayjs(startsAt).format('ddd D MMM YYYY, HH:mm')}–${dayjs(endsAt).format('HH:mm')}`
}

// Q77: a count, not a score, so working it out here is fine.
export function freeSpots(activity: Pick<Activity, 'spotsNeeded' | 'acceptedCount'>): number {
  return Math.max(0, activity.spotsNeeded - activity.acceptedCount)
}

export function spotsText(activity: Pick<Activity, 'spotsNeeded' | 'acceptedCount'>): string {
  const free = freeSpots(activity)
  return free === 0 ? `Full (${activity.spotsNeeded} spots)` : `${free} of ${activity.spotsNeeded} spots free`
}

export function placeText(activity: Pick<Activity, 'city' | 'fullAddress'>): string | null {
  return [activity.fullAddress, activity.city].filter(Boolean).join(', ') || null
}
