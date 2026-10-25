import type { Certification, HeldCertificationRequest } from '../api/types'

export type CertificationRow = { key: number; certificationId: string | null; validUntil: string | null }

let nextKey = 0
export const newKey = () => nextKey++

// Each certification can be chosen in one row only (Q73): a row offers the free ones and its own.
export function optionsForRow(all: Certification[], rows: CertificationRow[], row: CertificationRow) {
  const takenByOthers = new Set(rows.filter((r) => r.key !== row.key).map((r) => r.certificationId))
  return all
    .filter((c) => !takenByOthers.has(String(c.id)))
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((c) => ({ value: String(c.id), label: c.name }))
}

// Rows without a chosen certification are left out; validUntil is optional (no expiry).
export function toCertificationsRequest(rows: CertificationRow[]): HeldCertificationRequest[] {
  return rows
    .filter((r) => r.certificationId !== null)
    .map((r) => ({ certificationId: Number(r.certificationId), validUntil: r.validUntil ?? undefined }))
}
