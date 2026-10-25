import { describe, expect, it } from 'vitest'
import { optionsForRow, toCertificationsRequest, type CertificationRow } from './certifications'

const all = [
  { id: 1, name: 'First aid' },
  { id: 2, name: 'Driving licence B' },
  { id: 3, name: 'Food hygiene certificate' },
]

describe('certification rows', () => {
  it('offers each certification in one row only, keeping the row its own choice', () => {
    const rows: CertificationRow[] = [
      { key: 1, certificationId: '1', validUntil: null },
      { key: 2, certificationId: '2', validUntil: null },
    ]

    expect(optionsForRow(all, rows, rows[0]).map((o) => o.label)).toEqual(['First aid', 'Food hygiene certificate'])
    expect(optionsForRow(all, rows, rows[1]).map((o) => o.label)).toEqual(['Driving licence B', 'Food hygiene certificate'])
  })

  it('sends chosen rows with an optional expiry and skips empty rows', () => {
    const request = toCertificationsRequest([
      { key: 1, certificationId: '1', validUntil: '2027-05-31' },
      { key: 2, certificationId: null, validUntil: null },
      { key: 3, certificationId: '3', validUntil: null },
    ])

    expect(request).toEqual([
      { certificationId: 1, validUntil: '2027-05-31' },
      { certificationId: 3, validUntil: undefined },
    ])
  })
})
