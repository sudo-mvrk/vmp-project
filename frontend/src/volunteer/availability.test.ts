import { describe, expect, it } from 'vitest'
import { slotError, toAvailabilityRequest, toRows, type SlotRow } from './availability'

const row = (patch: Partial<SlotRow>): SlotRow => ({ key: 0, dayOfWeek: 'MONDAY', start: '09:00', end: '17:00', ...patch })

describe('availability', () => {
  it('loads slots as HH:mm rows sorted by day and start', () => {
    const rows = toRows([
      { dayOfWeek: 'SUNDAY', start: '08:00:00', end: '12:00:00' },
      { dayOfWeek: 'MONDAY', start: '14:00:00', end: '18:00:00' },
      { dayOfWeek: 'MONDAY', start: '08:00:00', end: '12:00:00' },
    ])

    expect(rows.map(({ dayOfWeek, start, end }) => [dayOfWeek, start, end])).toEqual([
      ['MONDAY', '08:00', '12:00'],
      ['MONDAY', '14:00', '18:00'],
      ['SUNDAY', '08:00', '12:00'],
    ])
  })

  it('accepts a slot that ends after it starts, up to 23:59', () => {
    expect(slotError(row({}))).toBeNull()
    expect(slotError(row({ start: '00:00', end: '23:59' }))).toBeNull()
  })

  it('rejects an end before or at the start', () => {
    expect(slotError(row({ start: '17:00', end: '09:00' }))).toBe('The end must be after the start')
    expect(slotError(row({ start: '09:00', end: '09:00' }))).toBe('The end must be after the start')
  })

  it('rejects empty times', () => {
    expect(slotError(row({ end: '' }))).toBe('Enter start and end')
  })

  it('leaves overlapping slots to the backend', () => {
    expect(slotError(row({ start: '09:00', end: '12:00' }))).toBeNull()
    expect(slotError(row({ start: '10:00', end: '13:00' }))).toBeNull()
  })

  it('sends the rows sorted, without the row keys', () => {
    const request = toAvailabilityRequest([
      row({ key: 1, dayOfWeek: 'FRIDAY' }),
      row({ key: 2, dayOfWeek: 'TUESDAY', start: '07:00', end: '08:00' }),
    ])

    expect(request).toEqual([
      { dayOfWeek: 'TUESDAY', start: '07:00', end: '08:00' },
      { dayOfWeek: 'FRIDAY', start: '09:00', end: '17:00' },
    ])
  })
})
