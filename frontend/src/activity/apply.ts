import type { Activity, ApplicationStatus, MyApplication } from '../api/types'
import type { Role } from '../auth/session'
import { freeSpots } from './format'

export type ApplyState =
  | { kind: 'login' }
  | { kind: 'hidden' }
  | { kind: 'closed' }
  | { kind: 'full' }
  | { kind: 'applied'; status: ApplicationStatus }
  | { kind: 'apply' }

// What the apply control shows (Q78, Q79). Only a guide: the backend checks every rule again
// and its 409 (started, full, missing certification) is shown as a message.
export function applyStateOf(
  role: Role | null,
  activity: Pick<Activity, 'id' | 'status' | 'spotsNeeded' | 'acceptedCount'>,
  myApplications: MyApplication[] | undefined,
): ApplyState {
  if (role === null) {
    return { kind: 'login' }
  }
  if (role !== 'VOLUNTEER') {
    return { kind: 'hidden' }
  }
  // One application per volunteer and activity, ever (Q5b), so any row means "already applied".
  const mine = myApplications?.find((application) => application.activityId === activity.id)
  if (mine) {
    return { kind: 'applied', status: mine.status }
  }
  if (activity.status !== 'OPEN') {
    return { kind: 'closed' }
  }
  if (freeSpots(activity) === 0) {
    return { kind: 'full' }
  }
  return { kind: 'apply' }
}
