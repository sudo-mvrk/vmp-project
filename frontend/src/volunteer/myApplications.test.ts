import { describe, expect, it } from 'vitest'
import { myApplication } from '../test/fixtures'
import { isCurrent, ownActionsOf, pendingInvitationCount } from './myApplications'

// The fixture's activity starts on 2026-11-07 09:00.
const before = new Date('2026-11-07T08:00:00')
const after = new Date('2026-11-07T09:30:00')
const invitation = (overrides = {}) => myApplication({ origin: 'INVITATION', ...overrides })

describe('ownActionsOf', () => {
  it('lets a pending application be withdrawn at any time', () => {
    expect(ownActionsOf(myApplication(), before)).toEqual({ kind: 'withdraw', accepted: false })
    expect(ownActionsOf(myApplication(), after)).toEqual({ kind: 'withdraw', accepted: false })
  })

  it('lets an accepted application be withdrawn only before the start', () => {
    expect(ownActionsOf(myApplication({ status: 'ACCEPTED' }), before)).toEqual({ kind: 'withdraw', accepted: true })
    expect(ownActionsOf(myApplication({ status: 'ACCEPTED' }), after)).toEqual({ kind: 'started' })
  })

  it('lets a pending invitation be accepted or declined, accept only before the start', () => {
    expect(ownActionsOf(invitation(), before)).toEqual({ kind: 'invitation', acceptBlocked: null })
    expect(ownActionsOf(invitation(), after)).toEqual({ kind: 'invitation', acceptBlocked: 'The activity has started' })
  })

  it('offers nothing on a cancelled or completed activity, or once decided', () => {
    expect(ownActionsOf(invitation({ activityStatus: 'CANCELLED' }), before)).toEqual({ kind: 'none' })
    expect(ownActionsOf(myApplication({ status: 'ACCEPTED', activityStatus: 'COMPLETED' }), after)).toEqual({ kind: 'none' })
    expect(ownActionsOf(myApplication({ status: 'REJECTED' }), before)).toEqual({ kind: 'none' })
    expect(ownActionsOf(invitation({ status: 'DECLINED' }), before)).toEqual({ kind: 'none' })
  })
})

describe('isCurrent', () => {
  it('keeps pending and accepted rows of activities that are not over', () => {
    expect(isCurrent(myApplication())).toBe(true)
    expect(isCurrent(myApplication({ status: 'ACCEPTED' }))).toBe(true)
    expect(isCurrent(myApplication({ status: 'WITHDRAWN' }))).toBe(false)
    expect(isCurrent(myApplication({ status: 'ACCEPTED', activityStatus: 'COMPLETED' }))).toBe(false)
    expect(isCurrent(myApplication({ activityStatus: 'CANCELLED' }))).toBe(false)
  })
})

describe('pendingInvitationCount', () => {
  it('counts pending invitations that can still be accepted', () => {
    const rows = [
      invitation({ id: 1 }),
      invitation({ id: 2, status: 'ACCEPTED' }),
      invitation({ id: 3, activityStatus: 'CANCELLED' }),
      myApplication({ id: 4 }),
    ]
    expect(pendingInvitationCount(rows, before)).toBe(1)
    expect(pendingInvitationCount(rows, after)).toBe(0)
    expect(pendingInvitationCount(undefined, before)).toBe(0)
  })
})
