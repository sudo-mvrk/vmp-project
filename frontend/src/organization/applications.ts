import type { Activity, ActivityApplication, ApplicationStatus, VolunteerProfile } from '../api/types'
import { freeSpots } from '../activity/format'
import { hasStarted } from './activityStatus'

const STATUS_ORDER: ApplicationStatus[] = ['PENDING', 'ACCEPTED', 'REJECTED', 'DECLINED', 'WITHDRAWN']

// Q99: pending first (they need a decision), then accepted, then the rest; each group by time of applying.
export function sortApplications(applications: ActivityApplication[]): ActivityApplication[] {
  return [...applications].sort(
    (a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status) || a.appliedAt.localeCompare(b.appliedAt),
  )
}

// Why the backend would refuse an accept now (ApplicationService.acceptUnderLocks), or null if it may succeed.
// Capacity and overlaps of the volunteer are not known here; their 409 is shown as a message.
export function acceptBlockedReason(
  activity: Pick<Activity, 'status' | 'startsAt' | 'spotsNeeded' | 'acceptedCount'>,
  now: Date,
): string | null {
  if (activity.status !== 'OPEN') return `The activity is ${activity.status.toLowerCase()}`
  if (hasStarted(activity, now)) return 'The activity has started'
  if (freeSpots(activity) === 0) return 'No free spots'
  return null
}

export type RowActions =
  | { kind: 'decide'; acceptBlocked: string | null }
  | { kind: 'waiting' }
  | { kind: 'none' }

// Q100: the organization decides applications; an invitation is decided by the volunteer (Q5i).
// Reject needs only a pending application, so it stays available after the start.
export function rowActionsOf(
  application: Pick<ActivityApplication, 'status' | 'origin'>,
  activity: Pick<Activity, 'status' | 'startsAt' | 'spotsNeeded' | 'acceptedCount'>,
  now: Date,
): RowActions {
  if (application.status !== 'PENDING') return { kind: 'none' }
  if (application.origin === 'INVITATION') return { kind: 'waiting' }
  return { kind: 'decide', acceptBlocked: acceptBlockedReason(activity, now) }
}

// Q105: recommendations need an open activity that has not started (Q6a); asking otherwise is a 409.
export function canRecommend(activity: Pick<Activity, 'status' | 'startsAt'>, now: Date): boolean {
  return activity.status === 'OPEN' && !hasStarted(activity, now)
}

export const fullName = (volunteer: Pick<VolunteerProfile, 'firstName' | 'lastName'>) =>
  `${volunteer.firstName} ${volunteer.lastName}`
