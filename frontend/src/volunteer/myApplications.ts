import dayjs from 'dayjs'
import type { MyApplication } from '../api/types'

const started = (application: Pick<MyApplication, 'activityStartsAt'>, now: Date) =>
  !dayjs(application.activityStartsAt).isAfter(dayjs(now))

const activityClosed = (application: Pick<MyApplication, 'activityStatus'>) =>
  application.activityStatus === 'CANCELLED' || application.activityStatus === 'COMPLETED'

// Q109: "current" rows still matter: pending or accepted, on an activity that is not over.
export function isCurrent(application: Pick<MyApplication, 'status' | 'activityStatus'>): boolean {
  return (application.status === 'PENDING' || application.status === 'ACCEPTED') && !activityClosed(application)
}

export type OwnRowActions =
  | { kind: 'withdraw'; accepted: boolean }
  | { kind: 'invitation'; acceptBlocked: string | null }
  | { kind: 'started' }
  | { kind: 'none' }

// Q110, Q112: what the volunteer can do with one row. Mirrors ActivityApplication and ApplicationService;
// free spots, capacity and overlaps are not known here, their 409 is shown as a message.
export function ownActionsOf(
  application: Pick<MyApplication, 'status' | 'origin' | 'activityStatus' | 'activityStartsAt'>,
  now: Date,
): OwnRowActions {
  if (activityClosed(application)) return { kind: 'none' }
  if (application.status === 'PENDING' && application.origin === 'INVITATION') {
    let acceptBlocked: string | null = null
    if (application.activityStatus !== 'OPEN') acceptBlocked = `The activity is ${application.activityStatus.toLowerCase()}`
    else if (started(application, now)) acceptBlocked = 'The activity has started'
    return { kind: 'invitation', acceptBlocked }
  }
  if (application.status === 'PENDING') return { kind: 'withdraw', accepted: false }
  if (application.status === 'ACCEPTED') {
    return started(application, now) ? { kind: 'started' } : { kind: 'withdraw', accepted: true }
  }
  return { kind: 'none' }
}

// Q114: invitations that wait for the volunteer's answer and can still be accepted.
export function pendingInvitationCount(applications: MyApplication[] | undefined, now: Date): number {
  return (applications ?? []).filter(
    (application) =>
      application.origin === 'INVITATION' &&
      application.status === 'PENDING' &&
      application.activityStatus === 'OPEN' &&
      !started(application, now),
  ).length
}
