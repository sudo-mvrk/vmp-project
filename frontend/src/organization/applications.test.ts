import { describe, expect, it } from 'vitest'
import { activity, activityApplication } from '../test/fixtures'
import { acceptBlockedReason, canRecommend, rowActionsOf, sortApplications } from './applications'

const before = new Date('2026-11-07T08:00:00')
const after = new Date('2026-11-07T09:30:00')

describe('sortApplications', () => {
  it('puts pending first, then accepted, then the rest, each by time of applying', () => {
    const sorted = sortApplications([
      activityApplication({ id: 1, status: 'REJECTED', appliedAt: '2026-10-20T10:00:00' }),
      activityApplication({ id: 2, status: 'ACCEPTED', appliedAt: '2026-10-21T10:00:00' }),
      activityApplication({ id: 3, status: 'PENDING', appliedAt: '2026-10-25T10:00:00' }),
      activityApplication({ id: 4, status: 'PENDING', appliedAt: '2026-10-22T10:00:00' }),
    ])

    expect(sorted.map((a) => a.id)).toEqual([4, 3, 2, 1])
  })
})

describe('rowActionsOf', () => {
  it('lets the organization decide a pending application', () => {
    expect(rowActionsOf(activityApplication(), activity(), before)).toEqual({ kind: 'decide', acceptBlocked: null })
  })

  it('leaves a pending invitation to the volunteer', () => {
    expect(rowActionsOf(activityApplication({ origin: 'INVITATION' }), activity(), before)).toEqual({ kind: 'waiting' })
  })

  it('offers nothing once decided', () => {
    for (const status of ['ACCEPTED', 'REJECTED', 'WITHDRAWN', 'DECLINED'] as const) {
      expect(rowActionsOf(activityApplication({ status }), activity(), before)).toEqual({ kind: 'none' })
    }
  })

  it('keeps reject but blocks accept after the start, when full or not open', () => {
    expect(rowActionsOf(activityApplication(), activity(), after)).toEqual({
      kind: 'decide',
      acceptBlocked: 'The activity has started',
    })
    expect(acceptBlockedReason(activity({ spotsNeeded: 2, acceptedCount: 2 }), before)).toBe('No free spots')
    expect(acceptBlockedReason(activity({ status: 'CANCELLED' }), before)).toBe('The activity is cancelled')
  })
})

describe('canRecommend', () => {
  it('needs an open activity that has not started', () => {
    expect(canRecommend(activity(), before)).toBe(true)
    expect(canRecommend(activity(), after)).toBe(false)
    expect(canRecommend(activity({ status: 'COMPLETED' }), before)).toBe(false)
  })
})
