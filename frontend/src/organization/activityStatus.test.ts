import { describe, expect, it } from 'vitest'
import { activity } from '../test/fixtures'
import { actionsOf, matchesFilter } from './activityStatus'

const before = new Date('2026-11-07T08:00:00')
const after = new Date('2026-11-07T09:30:00')

describe('actionsOf', () => {
  it('lets a draft be edited, published or cancelled', () => {
    expect(actionsOf(activity({ status: 'DRAFT' }), before)).toEqual(['edit', 'publish', 'cancel'])
  })

  it('lets an open activity be edited or cancelled before it starts', () => {
    expect(actionsOf(activity({ status: 'OPEN' }), before)).toEqual(['edit', 'cancel'])
  })

  it('lets an open activity be completed or cancelled once it has started', () => {
    expect(actionsOf(activity({ status: 'OPEN' }), after)).toEqual(['complete', 'cancel'])
    expect(actionsOf(activity({ status: 'OPEN' }), new Date('2026-11-07T09:00:00'))).toEqual(['complete', 'cancel'])
  })

  it('offers nothing for a completed or cancelled activity', () => {
    expect(actionsOf(activity({ status: 'COMPLETED' }), after)).toEqual([])
    expect(actionsOf(activity({ status: 'CANCELLED' }), before)).toEqual([])
  })
})

describe('matchesFilter', () => {
  it('keeps drafts and open activities, also started ones, under "active"', () => {
    expect(matchesFilter(activity({ status: 'DRAFT' }), 'active')).toBe(true)
    expect(matchesFilter(activity({ status: 'OPEN' }), 'active')).toBe(true)
    expect(matchesFilter(activity({ status: 'COMPLETED' }), 'active')).toBe(false)
  })

  it('puts completed and cancelled under "finished", and everything under "all"', () => {
    expect(matchesFilter(activity({ status: 'CANCELLED' }), 'finished')).toBe(true)
    expect(matchesFilter(activity({ status: 'OPEN' }), 'finished')).toBe(false)
    expect(matchesFilter(activity({ status: 'OPEN' }), 'all')).toBe(true)
  })
})
