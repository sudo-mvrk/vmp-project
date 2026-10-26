import { describe, expect, it } from 'vitest'
import { activity } from '../test/fixtures'
import { formatWindow, freeSpots, placeText, spotsText } from './format'

describe('formatWindow', () => {
  it('shows the backend clock time without converting zones', () => {
    expect(formatWindow('2026-11-07T09:00:00', '2026-11-07T12:30:00')).toBe('Sat 7 Nov 2026, 09:00–12:30')
  })
})

describe('free spots', () => {
  it('are the spots needed minus the accepted volunteers', () => {
    expect(freeSpots(activity({ spotsNeeded: 4, acceptedCount: 1 }))).toBe(3)
    expect(spotsText(activity({ spotsNeeded: 4, acceptedCount: 1 }))).toBe('3 of 4 spots free')
  })

  it('never go below zero and say "Full"', () => {
    expect(freeSpots(activity({ spotsNeeded: 2, acceptedCount: 3 }))).toBe(0)
    expect(spotsText(activity({ spotsNeeded: 2, acceptedCount: 2 }))).toBe('Full (2 spots)')
  })
})

describe('placeText', () => {
  it('joins the address parts that are present', () => {
    expect(placeText({ fullAddress: 'Na Poříčí 1', city: 'Prague' })).toBe('Na Poříčí 1, Prague')
    expect(placeText({ fullAddress: null, city: 'Prague' })).toBe('Prague')
    expect(placeText({ fullAddress: null, city: null })).toBeNull()
  })
})
