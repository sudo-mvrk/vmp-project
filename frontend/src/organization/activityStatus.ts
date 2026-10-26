import dayjs from 'dayjs'
import type { Activity, ActivityStatus } from '../api/types'

export type OwnerAction = 'edit' | 'publish' | 'cancel' | 'complete'

// Read with the browser's clock, only to choose the buttons (Q95); the backend decides with its own clock.
export function hasStarted(activity: Pick<Activity, 'startsAt'>, now: Date): boolean {
  return !dayjs(activity.startsAt).isAfter(dayjs(now))
}

// Q95: what the owner can do in each status. Mirrors Activity and ActivityService of the backend.
export function actionsOf(activity: Pick<Activity, 'status' | 'startsAt'>, now: Date): OwnerAction[] {
  switch (activity.status) {
    case 'DRAFT':
      return ['edit', 'publish', 'cancel']
    case 'OPEN':
      return hasStarted(activity, now) ? ['complete', 'cancel'] : ['edit', 'cancel']
    default:
      return []
  }
}

// Q88: the list filter. Started open activities stay "active" until they are completed.
export type StatusFilter = 'active' | 'finished' | 'all'

const ACTIVE: ActivityStatus[] = ['DRAFT', 'OPEN', 'IN_PROGRESS']

export function matchesFilter(activity: Pick<Activity, 'status'>, filter: StatusFilter): boolean {
  if (filter === 'all') return true
  return ACTIVE.includes(activity.status) === (filter === 'active')
}

export const statusColor: Record<ActivityStatus, string> = {
  DRAFT: 'gray',
  OPEN: 'green',
  IN_PROGRESS: 'blue',
  COMPLETED: 'teal',
  CANCELLED: 'red',
}
