import { describe, expect, it } from 'vitest'
import { activity, myApplication } from '../test/fixtures'
import { applyStateOf } from './apply'

describe('applyStateOf', () => {
  it('asks an anonymous user to log in', () => {
    expect(applyStateOf(null, activity(), undefined)).toEqual({ kind: 'login' })
  })

  it('hides the control from organizations and admins', () => {
    expect(applyStateOf('ORGANIZATION', activity(), undefined)).toEqual({ kind: 'hidden' })
    expect(applyStateOf('ADMIN', activity(), undefined)).toEqual({ kind: 'hidden' })
  })

  it('shows the status of an existing application, also a withdrawn one (one per activity, ever)', () => {
    expect(applyStateOf('VOLUNTEER', activity(), [myApplication({ status: 'WITHDRAWN' })])).toEqual({
      kind: 'applied',
      status: 'WITHDRAWN',
    })
  })

  it('ignores applications to other activities', () => {
    expect(applyStateOf('VOLUNTEER', activity(), [myApplication({ activityId: 99 })])).toEqual({ kind: 'apply' })
  })

  it('offers no apply for an activity that is not open or is full', () => {
    expect(applyStateOf('VOLUNTEER', activity({ status: 'CANCELLED' }), [])).toEqual({ kind: 'closed' })
    expect(applyStateOf('VOLUNTEER', activity({ spotsNeeded: 2, acceptedCount: 2 }), [])).toEqual({ kind: 'full' })
  })
})
