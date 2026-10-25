import type { AvailabilitySlot, DayOfWeek } from '../api/types'

export const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

export const dayLabel = (day: DayOfWeek) => day.charAt(0) + day.slice(1).toLowerCase()

// Times are edited as "HH:mm"; the backend sends "HH:mm:ss" and accepts both.
export type SlotRow = { key: number; dayOfWeek: DayOfWeek; start: string; end: string }

// Q3d: a slot cannot end at 24:00, the latest end is 23:59.
export const LATEST_END = '23:59'

const toMinutesPrecision = (time: string) => time.slice(0, 5)

let nextKey = 0
export const newSlotKey = () => nextKey++

function compareSlots(a: { dayOfWeek: DayOfWeek; start: string }, b: { dayOfWeek: DayOfWeek; start: string }) {
  return DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek) || a.start.localeCompare(b.start)
}

// Sorted by day and time when loaded and when saved (Q74), not while editing, so rows do not jump.
export function toRows(slots: AvailabilitySlot[]): SlotRow[] {
  return [...slots]
    .map((s) => ({ key: newSlotKey(), dayOfWeek: s.dayOfWeek, start: toMinutesPrecision(s.start), end: toMinutesPrecision(s.end) }))
    .sort(compareSlots)
}

// Only the checks of one row (Q74); overlapping slots are found by the backend (AvailabilityRules).
export function slotError(row: SlotRow): string | null {
  if (!row.start || !row.end) {
    return 'Enter start and end'
  }
  if (row.end > LATEST_END) {
    return 'The latest end is 23:59'
  }
  // "HH:mm" strings compare like times.
  if (row.end <= row.start) {
    return 'The end must be after the start'
  }
  return null
}

export function toAvailabilityRequest(rows: SlotRow[]): AvailabilitySlot[] {
  return [...rows].sort(compareSlots).map(({ dayOfWeek, start, end }) => ({ dayOfWeek, start, end }))
}
