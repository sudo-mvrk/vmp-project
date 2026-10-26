import type { ApplicationStatus, Priority } from '../api/types'

// Q130: muted tones of the theme (theme.ts), no bright signal colours.
export const priorityColor: Record<Priority, string> = {
  LOW: 'gray',
  MEDIUM: 'navy',
  HIGH: 'amber',
  URGENT: 'wine',
}

export const applicationStatusColor: Record<ApplicationStatus, string> = {
  PENDING: 'amber',
  ACCEPTED: 'sage',
  REJECTED: 'wine',
  WITHDRAWN: 'gray',
  DECLINED: 'gray',
}
